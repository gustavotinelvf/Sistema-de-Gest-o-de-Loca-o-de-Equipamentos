-- =====================================================================
-- ZipLoc | V1__schema_inicial.sql
-- PostgreSQL 16 | Gerenciado exclusivamente pelo Flyway
-- Escopo: locadora, cliente, usuario, modelo_equipamento,
--         unidade_equipamento, contrato, item_contrato.
-- Fora desta migration (virao em V2+): convite, solicitacao,
--         item_solicitacao, devolucao, vistoria, manutencao, fatura.
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS btree_gist;

-- ---------------------------------------------------------------------
-- LOCADORA (configuracao global: uma unica linha)
-- id nao usa IDENTITY de proposito: com CHECK (id = 1) e DEFAULT 1, uma
-- insercao rejeitada nao "queima" a sequencia e a linha unica continua
-- possivel.
-- ---------------------------------------------------------------------
CREATE TABLE locadora (
    id                      BIGINT        NOT NULL DEFAULT 1,
    razao_social            VARCHAR(200)  NOT NULL,
    cnpj                    VARCHAR(14)   NOT NULL,
    cep                     VARCHAR(8)    NOT NULL,
    logradouro              VARCHAR(200)  NOT NULL,
    numero                  VARCHAR(20)   NOT NULL,
    complemento             VARCHAR(100),
    bairro                  VARCHAR(100)  NOT NULL,
    cidade                  VARCHAR(100)  NOT NULL,
    uf                      CHAR(2)       NOT NULL,
    raio_entrega_km         NUMERIC(5,2)  NOT NULL,
    percentual_multa_padrao NUMERIC(5,2)  NOT NULL,
    data_cadastro           TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT pk_locadora PRIMARY KEY (id),
    CONSTRAINT ck_locadora_unica CHECK (id = 1),
    CONSTRAINT uk_locadora_cnpj UNIQUE (cnpj),
    CONSTRAINT ck_locadora_cnpj_formato CHECK (cnpj ~ '^[0-9]{14}$'),
    CONSTRAINT ck_locadora_cep_formato CHECK (cep ~ '^[0-9]{8}$'),
    CONSTRAINT ck_locadora_uf CHECK (uf IN (
        'AC','AL','AP','AM','BA','CE','DF','ES','GO','MA','MT','MS','MG','PA',
        'PB','PR','PE','PI','RJ','RN','RS','RO','RR','SC','SP','SE','TO')),
    CONSTRAINT ck_locadora_raio CHECK (raio_entrega_km > 0),
    CONSTRAINT ck_locadora_multa CHECK (percentual_multa_padrao >= 0)
);

-- ---------------------------------------------------------------------
-- CLIENTE (inquilino PJ)
-- ---------------------------------------------------------------------
CREATE TABLE cliente (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY,
    razao_social       VARCHAR(200) NOT NULL,
    cnpj               VARCHAR(14)  NOT NULL,
    contato_nome       VARCHAR(200) NOT NULL,
    email              VARCHAR(200) NOT NULL,
    telefone           VARCHAR(20)  NOT NULL,
    cep                VARCHAR(8)   NOT NULL,
    logradouro         VARCHAR(200) NOT NULL,
    numero             VARCHAR(20)  NOT NULL,
    complemento        VARCHAR(100),
    bairro             VARCHAR(100) NOT NULL,
    cidade             VARCHAR(100) NOT NULL,
    uf                 CHAR(2)      NOT NULL,
    status_adimplencia VARCHAR(15)  NOT NULL DEFAULT 'ADIMPLENTE',
    ativo              BOOLEAN      NOT NULL DEFAULT TRUE,
    data_cadastro      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    anonimizado_em     TIMESTAMPTZ,
    CONSTRAINT pk_cliente PRIMARY KEY (id),
    CONSTRAINT uk_cliente_cnpj UNIQUE (cnpj),
    CONSTRAINT uk_cliente_email UNIQUE (email),
    CONSTRAINT ck_cliente_cnpj_formato CHECK (cnpj ~ '^[0-9]{14}$'),
    CONSTRAINT ck_cliente_cep_formato CHECK (cep ~ '^[0-9]{8}$'),
    CONSTRAINT ck_cliente_uf CHECK (uf IN (
        'AC','AL','AP','AM','BA','CE','DF','ES','GO','MA','MT','MS','MG','PA',
        'PB','PR','PE','PI','RJ','RN','RS','RO','RR','SC','SP','SE','TO')),
    CONSTRAINT ck_cliente_status_adimplencia
        CHECK (status_adimplencia IN ('ADIMPLENTE', 'INADIMPLENTE'))
);

CREATE INDEX ix_cliente_status_adimplencia ON cliente (status_adimplencia);

-- ---------------------------------------------------------------------
-- USUARIO (acesso e perfis)
-- Cargo = perfil. cpf obrigatorio apenas para GERENTE (RD016).
-- ---------------------------------------------------------------------
CREATE TABLE usuario (
    id             BIGINT GENERATED ALWAYS AS IDENTITY,
    nome           VARCHAR(200) NOT NULL,
    email          VARCHAR(200) NOT NULL,
    senha_hash     VARCHAR(255) NOT NULL,
    perfil         VARCHAR(20)  NOT NULL,
    cpf            VARCHAR(11),
    status         VARCHAR(10)  NOT NULL DEFAULT 'ATIVO',
    data_cadastro  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    data_ativacao  TIMESTAMPTZ,
    anonimizado_em TIMESTAMPTZ,
    CONSTRAINT pk_usuario PRIMARY KEY (id),
    CONSTRAINT uk_usuario_email UNIQUE (email),
    CONSTRAINT uk_usuario_cpf UNIQUE (cpf),
    CONSTRAINT ck_usuario_perfil CHECK (perfil IN (
        'GERENTE', 'FINANCEIRO', 'ATENDENTE', 'ESTOQUISTA', 'TECNICO')),
    CONSTRAINT ck_usuario_status CHECK (status IN ('ATIVO', 'INATIVO')),
    CONSTRAINT ck_usuario_cpf_formato CHECK (cpf IS NULL OR cpf ~ '^[0-9]{11}$'),
    CONSTRAINT ck_usuario_cpf_gerente CHECK (perfil <> 'GERENTE' OR cpf IS NOT NULL)
);

-- ---------------------------------------------------------------------
-- CATALOGO: modelo_equipamento (item de catalogo)
-- descricao cobre as especificacoes tecnicas do dicionario (D05).
-- ---------------------------------------------------------------------
CREATE TABLE modelo_equipamento (
    id                BIGINT GENERATED ALWAYS AS IDENTITY,
    nome              VARCHAR(200)  NOT NULL,
    categoria         VARCHAR(100)  NOT NULL,
    descricao         TEXT,
    valor_diaria_base NUMERIC(10,2) NOT NULL,
    quantidade_minima INTEGER       NOT NULL DEFAULT 0,
    quantidade_maxima INTEGER       NOT NULL DEFAULT 1,
    ativo             BOOLEAN       NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_modelo_equipamento PRIMARY KEY (id),
    CONSTRAINT ck_modelo_valor_diaria CHECK (valor_diaria_base > 0),
    CONSTRAINT ck_modelo_qtd_minima CHECK (quantidade_minima >= 0),
    CONSTRAINT ck_modelo_qtd_maxima CHECK (quantidade_maxima > quantidade_minima)
);

CREATE INDEX ix_modelo_equipamento_categoria ON modelo_equipamento (categoria);

-- ---------------------------------------------------------------------
-- ESTOQUE: unidade_equipamento (maquina fisica)
-- version = trava otimista (@Version no JPA).
-- Nota: RD013 do plano preve tambem o status EM_VISTORIA. Ele entra em
-- uma migration V2 quando o modulo de devolucao for criado.
-- ---------------------------------------------------------------------
CREATE TABLE unidade_equipamento (
    id           BIGINT GENERATED ALWAYS AS IDENTITY,
    modelo_id    BIGINT      NOT NULL,
    numero_serie VARCHAR(50) NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'DISPONIVEL',
    ativo        BOOLEAN     NOT NULL DEFAULT TRUE,
    version      BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT pk_unidade_equipamento PRIMARY KEY (id),
    CONSTRAINT uk_unidade_equipamento_numero_serie UNIQUE (numero_serie),
    CONSTRAINT fk_unidade_equipamento_modelo FOREIGN KEY (modelo_id)
        REFERENCES modelo_equipamento (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT ck_unidade_equipamento_status
        CHECK (status IN ('DISPONIVEL', 'LOCADA', 'EM_MANUTENCAO'))
);

CREATE INDEX ix_unidade_equipamento_modelo_status ON unidade_equipamento (modelo_id, status);

-- ---------------------------------------------------------------------
-- CONTRATO
-- percentual_multa e copiado da locadora na formalizacao (RD015).
-- UNIQUE (id, inicio, fim) existe para a FK composta de item_contrato.
-- Nota: o dicionario preve tambem o status CANCELADO (D08); entra em V2.
-- ---------------------------------------------------------------------
CREATE TABLE contrato (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY,
    cliente_id           BIGINT        NOT NULL,
    data_criacao         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    data_inicio_prevista DATE          NOT NULL,
    data_fim_prevista    DATE          NOT NULL,
    status               VARCHAR(20)   NOT NULL DEFAULT 'PLANEJADO',
    valor_total          NUMERIC(10,2) NOT NULL DEFAULT 0,
    percentual_multa     NUMERIC(5,2)  NOT NULL DEFAULT 0,
    version              BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_contrato PRIMARY KEY (id),
    CONSTRAINT uk_contrato_id_periodo UNIQUE (id, data_inicio_prevista, data_fim_prevista),
    CONSTRAINT fk_contrato_cliente FOREIGN KEY (cliente_id)
        REFERENCES cliente (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT ck_contrato_status CHECK (status IN ('PLANEJADO', 'ATIVO', 'FINALIZADO')),
    CONSTRAINT ck_contrato_periodo CHECK (data_fim_prevista > data_inicio_prevista),
    CONSTRAINT ck_contrato_valor_total CHECK (valor_total >= 0),
    CONSTRAINT ck_contrato_percentual_multa CHECK (percentual_multa >= 0)
);

CREATE INDEX ix_contrato_cliente ON contrato (cliente_id);
CREATE INDEX ix_contrato_status ON contrato (status);

-- Maquina de estados estrita: PLANEJADO -> ATIVO -> FINALIZADO.
CREATE FUNCTION fn_contrato_valida_transicao() RETURNS trigger AS $$
BEGIN
    IF NEW.status = OLD.status THEN
        RETURN NEW;
    END IF;
    IF (OLD.status = 'PLANEJADO' AND NEW.status = 'ATIVO')
       OR (OLD.status = 'ATIVO' AND NEW.status = 'FINALIZADO') THEN
        RETURN NEW;
    END IF;
    RAISE EXCEPTION 'Transicao de status invalida no contrato %: % -> %',
        OLD.id, OLD.status, NEW.status
        USING ERRCODE = 'check_violation';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tg_contrato_transicao_status
    BEFORE UPDATE OF status ON contrato
    FOR EACH ROW EXECUTE FUNCTION fn_contrato_valida_transicao();

-- RD006: contrato finalizado nao pode ser excluido.
CREATE FUNCTION fn_contrato_bloqueia_exclusao() RETURNS trigger AS $$
BEGIN
    IF OLD.status = 'FINALIZADO' THEN
        RAISE EXCEPTION 'Contrato % finalizado nao pode ser excluido', OLD.id
            USING ERRCODE = 'restrict_violation';
    END IF;
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tg_contrato_bloqueia_exclusao
    BEFORE DELETE ON contrato
    FOR EACH ROW EXECUTE FUNCTION fn_contrato_bloqueia_exclusao();

-- ---------------------------------------------------------------------
-- ITEM_CONTRATO (liga contrato a unidade fisica)
-- data_inicio_prevista/data_fim_prevista sao copias do contrato, mantidas
-- consistentes pela FK composta (necessarias para a exclusao por periodo,
-- pois uma EXCLUDE so enxerga colunas da propria tabela).
-- ---------------------------------------------------------------------
CREATE TABLE item_contrato (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY,
    contrato_id          BIGINT NOT NULL,
    unidade_id           BIGINT NOT NULL,
    data_inicio_prevista DATE   NOT NULL,
    data_fim_prevista    DATE   NOT NULL,
    data_inicio_real     DATE,
    data_devolucao_real  DATE,
    CONSTRAINT pk_item_contrato PRIMARY KEY (id),
    CONSTRAINT uk_item_contrato_contrato_unidade UNIQUE (contrato_id, unidade_id),
    CONSTRAINT fk_item_contrato_contrato FOREIGN KEY
        (contrato_id, data_inicio_prevista, data_fim_prevista)
        REFERENCES contrato (id, data_inicio_prevista, data_fim_prevista)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_item_contrato_unidade FOREIGN KEY (unidade_id)
        REFERENCES unidade_equipamento (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT ck_item_contrato_periodo CHECK (data_fim_prevista > data_inicio_prevista),
    CONSTRAINT ck_item_contrato_devolucao CHECK (
        data_devolucao_real IS NULL
        OR (data_inicio_real IS NOT NULL AND data_devolucao_real >= data_inicio_real)),
    -- Anti-overbooking: a mesma unidade nao pode ter periodos sobrepostos
    -- (inicio e fim inclusivos).
    CONSTRAINT ex_item_contrato_sem_overbooking EXCLUDE USING gist (
        unidade_id WITH =,
        daterange(data_inicio_prevista, data_fim_prevista, '[]') WITH &&)
);

CREATE INDEX ix_item_contrato_contrato ON item_contrato (contrato_id);
CREATE INDEX ix_item_contrato_unidade_periodo
    ON item_contrato (unidade_id, data_inicio_prevista, data_fim_prevista);