CREATE OR REPLACE FUNCTION registrar_manutencao()
RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    u unidade_equipamento%ROWTYPE;
BEGIN
    IF TG_OP = 'UPDATE' AND (
        OLD.data_conclusao IS NOT NULL OR
        (NEW.unidade_id, NEW.locadora_id)
        IS DISTINCT FROM
        (OLD.unidade_id, OLD.locadora_id)
    ) THEN
        RAISE EXCEPTION
            'Manutencao concluida ou unidade vinculada nao pode mudar';
    END IF;

    SELECT *
    INTO STRICT u
    FROM unidade_equipamento
    WHERE id = NEW.unidade_id
    FOR UPDATE;

    IF TG_OP = 'INSERT' THEN
        IF u.status IN ('LOCADA', 'EM_VISTORIA') THEN
            RAISE EXCEPTION
                'Unidade locada ou em vistoria nao pode entrar em manutencao';
        END IF;

        -- Manutenções preventivas devem respeitar as reservas existentes.
        -- Manutenções corretivas possuem prioridade operacional.
        IF NEW.tipo = 'PREVENTIVA' AND EXISTS (
            SELECT 1
            FROM item_contrato
            WHERE unidade_id = NEW.unidade_id
              AND status_contrato IN ('PLANEJADO', 'ATIVO')
              AND data_fim_prevista >=
                  (NEW.data_inicio AT TIME ZONE 'America/Sao_Paulo')::date
        ) THEN
            RAISE EXCEPTION
                'Resolva as reservas futuras antes de iniciar manutencao preventiva';
        END IF;
    END IF;

    UPDATE unidade_equipamento
    SET status = CASE
        WHEN NEW.data_conclusao IS NULL THEN 'EM_MANUTENCAO'
        ELSE 'DISPONIVEL'
    END
    WHERE id = NEW.unidade_id;

    RETURN NEW;
END $$;

-- Identifica reservas planejadas afetadas por manutenção em andamento.
CREATE VIEW vw_reservas_afetadas_manutencao AS
SELECT
    i.locadora_id,
    i.id AS item_contrato_id,
    i.contrato_id,
    c.cliente_id,
    i.modelo_id,
    i.unidade_id,
    u.numero_serie,
    i.data_inicio_prevista,
    i.data_fim_prevista,
    m.id AS manutencao_id,
    m.tipo AS tipo_manutencao,
    m.data_inicio AS inicio_manutencao,
    m.data_prevista AS previsao_conclusao
FROM item_contrato i
JOIN contrato c ON c.id = i.contrato_id
JOIN unidade_equipamento u ON u.id = i.unidade_id
JOIN manutencao m ON m.unidade_id = i.unidade_id
WHERE i.status_contrato = 'PLANEJADO'
  AND m.data_conclusao IS NULL;

GRANT SELECT ON vw_reservas_afetadas_manutencao
TO ziploc_equipe;