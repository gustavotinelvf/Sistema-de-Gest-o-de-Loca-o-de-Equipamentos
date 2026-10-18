-- =====================================================================
-- ZipLoc | V2__schema_final.sql
-- PostgreSQL 16 | Gerenciado exclusivamente pelo Flyway
-- Escopo: convite, solicitacao, item_solicitacao, devolucao, 
--         vistoria, manutencao, fatura.
-- =====================================================================

-- ---------------------------------------------------------------------
-- CONVITE (Acesso ao sistema para novos colaboradores)
-- ---------------------------------------------------------------------
CREATE TABLE convite (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    email VARCHAR(200) NOT NULL,
    token VARCHAR(255) NOT NULL,
    perfil VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    data_criacao TIMESTAMPTZ NOT NULL DEFAULT now(),
    data_expiracao TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_convite PRIMARY KEY (id),
    CONSTRAINT uk_convite_token UNIQUE (token),
    CONSTRAINT ck_convite_perfil CHECK (perfil IN ('GERENTE', 'FINANCEIRO', 'ATENDENTE', 'ESTOQUISTA', 'TECNICO')),
    CONSTRAINT ck_convite_status CHECK (status IN ('PENDENTE', 'ACEITO', 'EXPIRADO'))
);

-- ---------------------------------------------------------------------
-- SOLICITACAO (Pedido inicial do cliente, antes de virar contrato)
-- ---------------------------------------------------------------------
CREATE TABLE solicitacao (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    cliente_id BIGINT NOT NULL,
    data_criacao TIMESTAMPTZ NOT NULL DEFAULT now(),
    data_inicio_desejada DATE NOT NULL,
    data_fim_desejada DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    CONSTRAINT pk_solicitacao PRIMARY KEY (id),
    CONSTRAINT fk_solicitacao_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (id) ON DELETE RESTRICT,
    CONSTRAINT ck_solicitacao_status CHECK (status IN ('PENDENTE', 'APROVADA', 'REJEITADA', 'CANCELADA')),
    CONSTRAINT ck_solicitacao_periodo CHECK (data_fim_desejada > data_inicio_desejada)
);

-- ---------------------------------------------------------------------
-- ITEM_SOLICITACAO (Modelos desejados na solicitacao)
-- ---------------------------------------------------------------------
CREATE TABLE item_solicitacao (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    solicitacao_id BIGINT NOT NULL,
    modelo_id BIGINT NOT NULL,
    quantidade INTEGER NOT NULL,
    CONSTRAINT pk_item_solicitacao PRIMARY KEY (id),
    CONSTRAINT uk_item_sol_solicitacao_modelo UNIQUE (solicitacao_id, modelo_id),
    CONSTRAINT fk_item_sol_solicitacao FOREIGN KEY (solicitacao_id) REFERENCES solicitacao (id) ON DELETE CASCADE,
    CONSTRAINT fk_item_sol_modelo FOREIGN KEY (modelo_id) REFERENCES modelo_equipamento (id) ON DELETE RESTRICT,
    CONSTRAINT ck_item_sol_quantidade CHECK (quantidade > 0)
);

-- ---------------------------------------------------------------------
-- DEVOLUCAO (Registro de retorno fisico da unidade)
-- ---------------------------------------------------------------------
CREATE TABLE devolucao (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    item_contrato_id BIGINT NOT NULL,
    usuario_recebedor_id BIGINT NOT NULL,
    data_devolucao TIMESTAMPTZ NOT NULL DEFAULT now(),
    observacoes TEXT,
    CONSTRAINT pk_devolucao PRIMARY KEY (id),
    CONSTRAINT uk_devolucao_item_contrato UNIQUE (item_contrato_id),
    CONSTRAINT fk_devolucao_item_contrato FOREIGN KEY (item_contrato_id) REFERENCES item_contrato (id) ON DELETE RESTRICT,
    CONSTRAINT fk_devolucao_recebedor FOREIGN KEY (usuario_recebedor_id) REFERENCES usuario (id) ON DELETE RESTRICT
);

-- ---------------------------------------------------------------------
-- VISTORIA (Inspecao tecnica apos devolucao)
-- ---------------------------------------------------------------------
CREATE TABLE vistoria (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    devolucao_id BIGINT NOT NULL,
    usuario_tecnico_id BIGINT NOT NULL,
    data_vistoria TIMESTAMPTZ NOT NULL DEFAULT now(),
    status VARCHAR(20) NOT NULL,
    custo_dano NUMERIC(10,2) NOT NULL DEFAULT 0,
    laudo TEXT,
    CONSTRAINT pk_vistoria PRIMARY KEY (id),
    CONSTRAINT uk_vistoria_devolucao UNIQUE (devolucao_id),
    CONSTRAINT fk_vistoria_devolucao FOREIGN KEY (devolucao_id) REFERENCES devolucao (id) ON DELETE CASCADE,
    CONSTRAINT fk_vistoria_tecnico FOREIGN KEY (usuario_tecnico_id) REFERENCES usuario (id) ON DELETE RESTRICT,
    CONSTRAINT ck_vistoria_status CHECK (status IN ('APROVADA', 'REPROVADA_COM_DANOS')),
    CONSTRAINT ck_vistoria_custo CHECK (custo_dano >= 0)
);

-- ---------------------------------------------------------------------
-- MANUTENCAO (Conserto de equipamentos danificados)
-- ---------------------------------------------------------------------
CREATE TABLE manutencao (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    unidade_id BIGINT NOT NULL,
    data_inicio TIMESTAMPTZ NOT NULL DEFAULT now(),
    data_fim TIMESTAMPTZ,
    descricao_problema TEXT NOT NULL,
    custo_reparo NUMERIC(10,2) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'EM_ANDAMENTO',
    CONSTRAINT pk_manutencao PRIMARY KEY (id),
    CONSTRAINT fk_manutencao_unidade FOREIGN KEY (unidade_id) REFERENCES unidade_equipamento (id) ON DELETE RESTRICT,
    CONSTRAINT ck_manutencao_status CHECK (status IN ('AGUARDANDO', 'EM_ANDAMENTO', 'CONCLUIDA')),
    CONSTRAINT ck_manutencao_periodo CHECK (data_fim IS NULL OR data_fim >= data_inicio),
    CONSTRAINT ck_manutencao_custo CHECK (custo_reparo >= 0)
);

-- ---------------------------------------------------------------------
-- FATURA (Cobranca financeira atrelada ao contrato e vistorias)
-- ---------------------------------------------------------------------
CREATE TABLE fatura (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    contrato_id BIGINT NOT NULL,
    data_emissao TIMESTAMPTZ NOT NULL DEFAULT now(),
    data_vencimento DATE NOT NULL,
    data_pagamento TIMESTAMPTZ,
    valor_base NUMERIC(10,2) NOT NULL,
    valor_multa_atraso NUMERIC(10,2) NOT NULL DEFAULT 0,
    valor_danos NUMERIC(10,2) NOT NULL DEFAULT 0,
    valor_total NUMERIC(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    CONSTRAINT pk_fatura PRIMARY KEY (id),
    CONSTRAINT uk_fatura_contrato UNIQUE (contrato_id),
    CONSTRAINT fk_fatura_contrato FOREIGN KEY (contrato_id) REFERENCES contrato (id) ON DELETE RESTRICT,
    CONSTRAINT ck_fatura_status CHECK (status IN ('PENDENTE', 'PAGA', 'CANCELADA', 'VENCIDA')),
    CONSTRAINT ck_fatura_valores CHECK (
        valor_base >= 0 AND 
        valor_multa_atraso >= 0 AND 
        valor_danos >= 0 AND 
        valor_total = (valor_base + valor_multa_atraso + valor_danos)
    )
);