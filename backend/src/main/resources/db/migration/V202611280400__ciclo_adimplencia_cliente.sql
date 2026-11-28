-- RD010: ciclos de tolerancia e historico de inadimplencia.

ALTER TABLE cliente
    ADD COLUMN ciclo_adimplencia integer NOT NULL DEFAULT 1,
    ADD CONSTRAINT cliente_ciclo_adimplencia_check
        CHECK (ciclo_adimplencia >= 1);


-- Garante o vinculo entre contrato, cliente e locadora.

ALTER TABLE contrato
    ADD CONSTRAINT contrato_cliente_locadora_unique
        UNIQUE (id, cliente_id, locadora_id);


-- Cada contrato pode gerar somente uma ocorrencia de atraso,
-- independentemente da quantidade de equipamentos atrasados.

CREATE TABLE ocorrencia_atraso (

    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    locadora_id bigint NOT NULL,

    cliente_id bigint NOT NULL,

    contrato_id bigint NOT NULL UNIQUE,

    ciclo integer NOT NULL CHECK (ciclo >= 1),

    data_ocorrencia timestamptz NOT NULL
        DEFAULT current_timestamp,

    FOREIGN KEY (cliente_id, locadora_id)
        REFERENCES cliente(id, locadora_id),

    FOREIGN KEY (contrato_id, cliente_id, locadora_id)
        REFERENCES contrato(id, cliente_id, locadora_id)

);


CREATE INDEX ocorrencia_atraso_cliente_ciclo_idx
    ON ocorrencia_atraso(locadora_id, cliente_id, ciclo);


-- Historico das decisoes gerenciais de reconsideracao.

CREATE TABLE reconsideracao_cliente (

    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    locadora_id bigint NOT NULL,

    cliente_id bigint NOT NULL,

    gerente_id bigint NOT NULL,

    ciclo_anterior integer NOT NULL
        CHECK (ciclo_anterior >= 1),

    decisao varchar(10) NOT NULL
        CHECK (decisao IN ('APROVADA', 'REJEITADA')),

    justificativa text NOT NULL
        CHECK (btrim(justificativa) <> ''),

    data_decisao timestamptz NOT NULL
        DEFAULT current_timestamp,

    FOREIGN KEY (cliente_id, locadora_id)
        REFERENCES cliente(id, locadora_id),

    FOREIGN KEY (gerente_id, locadora_id)
        REFERENCES usuario(id, locadora_id)

);


CREATE INDEX reconsideracao_cliente_historico_idx
    ON reconsideracao_cliente(
        locadora_id,
        cliente_id,
        data_decisao
    );


-- Recupera ocorrencias anteriores a partir das devolucoes
-- existentes, sem duplicar contratos com varios itens atrasados.

INSERT INTO ocorrencia_atraso (
    locadora_id,
    cliente_id,
    contrato_id,
    ciclo,
    data_ocorrencia
)
SELECT
    c.locadora_id,
    c.cliente_id,
    c.id,
    1,
    MIN(d.data_devolucao_real)
FROM devolucao d
JOIN item_contrato i
    ON i.id = d.item_contrato_id
JOIN contrato c
    ON c.id = i.contrato_id
WHERE
    (d.data_devolucao_real AT TIME ZONE
        'America/Sao_Paulo')::date > c.data_fim_prevista
GROUP BY
    c.locadora_id,
    c.cliente_id,
    c.id;


-- Reclassifica clientes com atrasos historicos de acordo
-- com a regra de reincidencia.
-- Clientes sem ocorrencias registradas nao sao alterados.

UPDATE cliente cl
SET status_adimplencia = CASE
    WHEN historico.quantidade >= 2 THEN 'INADIMPLENTE'
    ELSE 'ADIMPLENTE'
END
FROM (
    SELECT
        cliente_id,
        locadora_id,
        COUNT(*) AS quantidade
    FROM ocorrencia_atraso
    WHERE ciclo = 1
    GROUP BY cliente_id, locadora_id
) historico
WHERE cl.id = historico.cliente_id
  AND cl.locadora_id = historico.locadora_id;


-- Substitui a funcao original de devolucao,
-- preservando suas validacoes e integracao com Vistoria.

CREATE OR REPLACE FUNCTION registrar_devolucao()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE

    i item_contrato%ROWTYPE;

    c contrato%ROWTYPE;

    ciclo_atual integer;

    quantidade_atrasos bigint;

BEGIN

    -- Localiza o item pertencente a locadora.

    SELECT *
    INTO STRICT i
    FROM item_contrato
    WHERE id = NEW.item_contrato_id
      AND locadora_id = NEW.locadora_id;


    -- Serializa devolucoes do mesmo contrato.

    SELECT *
    INTO STRICT c
    FROM contrato
    WHERE id = i.contrato_id
      AND locadora_id = NEW.locadora_id
    FOR UPDATE;


    -- Somente contratos ativos podem receber devolucoes.

    IF c.status <> 'ATIVO' THEN

        RAISE EXCEPTION
            'Devolucao exige contrato ATIVO';

    END IF;


    -- Impede devolucao anterior a retirada.

    IF NEW.data_devolucao_real < c.data_retirada THEN

        RAISE EXCEPTION
            'Devolucao anterior a retirada';

    END IF;


    -- Preserva o teto acumulado da multa contratual.

    IF NEW.multa_atraso + COALESCE(

        (
            SELECT SUM(d.multa_atraso)
            FROM devolucao d
            JOIN item_contrato ic
                ON ic.id = d.item_contrato_id
            WHERE ic.contrato_id = c.id
              AND ic.locadora_id = c.locadora_id
        ),

        0

    ) > c.valor_locacao THEN

        RAISE EXCEPTION
            'Soma de multas excede valor do contrato';

    END IF;


    -- Preserva a integracao com o modulo de Vistoria.

    PERFORM 1
    FROM unidade_equipamento
    WHERE id = i.unidade_id
      AND locadora_id = c.locadora_id
    FOR UPDATE;


    UPDATE unidade_equipamento
    SET status = 'EM_VISTORIA'
    WHERE id = i.unidade_id
      AND locadora_id = c.locadora_id;


    -- RD010: qualquer atraso caracteriza uma ocorrencia,
    -- independentemente da carencia para multa.

    IF (
        NEW.data_devolucao_real AT TIME ZONE
        'America/Sao_Paulo'
    )::date > c.data_fim_prevista THEN


        -- Serializa atrasos de contratos diferentes
        -- pertencentes ao mesmo cliente.

        SELECT ciclo_adimplencia
        INTO STRICT ciclo_atual
        FROM cliente
        WHERE id = c.cliente_id
          AND locadora_id = c.locadora_id
        FOR UPDATE;


        -- Um contrato gera somente uma ocorrencia.

        INSERT INTO ocorrencia_atraso (
            locadora_id,
            cliente_id,
            contrato_id,
            ciclo,
            data_ocorrencia
        )
        VALUES (
            c.locadora_id,
            c.cliente_id,
            c.id,
            ciclo_atual,
            NEW.data_devolucao_real
        )
        ON CONFLICT (contrato_id)
        DO NOTHING;


        -- Conta somente as ocorrencias do ciclo vigente.

        SELECT COUNT(*)
        INTO quantidade_atrasos
        FROM ocorrencia_atraso
        WHERE cliente_id = c.cliente_id
          AND locadora_id = c.locadora_id
          AND ciclo = ciclo_atual;


        -- A segunda ocorrencia caracteriza reincidencia.

        IF quantidade_atrasos >= 2 THEN

            UPDATE cliente
            SET status_adimplencia = 'INADIMPLENTE'
            WHERE id = c.cliente_id
              AND locadora_id = c.locadora_id;

        END IF;

    END IF;


    RETURN NEW;

END;
$$;

-- Permissoes para as novas tabelas de historico.

GRANT SELECT, INSERT
ON ocorrencia_atraso, reconsideracao_cliente
TO ziploc_equipe;

GRANT USAGE, SELECT
ON SEQUENCE
    ocorrencia_atraso_id_seq,
    reconsideracao_cliente_id_seq
TO ziploc_equipe;