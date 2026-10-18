-- =====================================================================
-- ZipLoc | V2__modulos_operacionais.sql
-- PostgreSQL 16 | Gerenciado exclusivamente pelo Flyway
-- Escopo: convite, solicitacao, item_solicitacao, devolucao,
--         vistoria, manutencao, fatura, e colunas novas em contrato.
-- Dependencias: tabelas criadas no V1.
-- Migrations seguintes devem usar timestamp no nome
-- (ex.: V202611021430__descricao.sql) para nao colidir entre equipes.
-- =====================================================================

-- ---------------------------------------------------------------------
-- CONVITE (acesso ao sistema para novos colaboradores)
-- ---------------------------------------------------------------------
CREATE TABLE convite (
    id                BIGINT GENERATED ALWAYS AS IDENTITY,
    email             VARCHAR(200) NOT NULL,
    token             VARCHAR(255) NOT NULL,
    perfil            VARCHAR(20)  NOT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'PENDENTE',
    convidado_por_id  BIGINT       NOT NULL,
    data_criacao      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    data_expiracao    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_convite PRIMARY KEY (id),
    CONSTRAINT uk_convite_token UNIQUE (token),
    CONSTRAINT fk_convite_convidado_por FOREIGN KEY (convidado_por_id)
        REFERENCES usuario (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT ck_convite_perfil CHECK (perfil IN (
        'GERENTE', 'FINANCEIRO', 'ATENDENTE', 'ESTOQUISTA', 'TECNICO')),
    CONSTRAINT ck_convite_status CHECK (status IN (
        'PENDENTE', 'ACEITO', 'EXPIRADO', 'CANCELADO')),
    CONSTRAINT ck_convite_validade CHECK (data_expiracao > data_criacao)
);

CREATE INDEX ix_convite_email_status ON convite (email, status);

-- ---------------------------------------------------------------------
-- SOLICITACAO (pedido do cliente, antes de virar contrato)
-- A distancia e calculada no registro da solicitacao (nao na consulta
-- de disponibilidade). Justificativa obrigatoria para rejeitar ou
-- cancelar (RD012).
-- ---------------------------------------------------------------------
CREATE TABLE solicitacao (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY,
    cliente_id            BIGINT        NOT NULL,
    registrado_por_id     BIGINT        NOT NULL,
    analisado_por_id      BIGINT,
    data_criacao          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    data_analise          TIMESTAMPTZ,
    data_inicio_desejada  DATE          NOT NULL,
    data_fim_desejada     DATE          NOT NULL,
    distancia_km          NUMERIC(7,2),
    distancia_verificada  BOOLEAN       NOT NULL DEFAULT FALSE,
    status                VARCHAR(20)   NOT NULL DEFAULT 'PENDENTE',
    justificativa         TEXT,
    CONSTRAINT pk_solicitacao PRIMARY KEY (id),
    CONSTRAINT fk_solicitacao_cliente FOREIGN KEY (cliente_id)
        REFERENCES cliente (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_solicitacao_registrado_por FOREIGN KEY (registrado_por_id)
        REFERENCES usuario (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_solicitacao_analisado_por FOREIGN KEY (analisado_por_id)
        REFERENCES usuario (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT ck_solicitacao_status CHECK (status IN (
        'PENDENTE', 'APROVADA', 'REJEITADA', 'CANCELADA')),
    CONSTRAINT ck_solicitacao_periodo CHECK (data_fim_desejada > data_inicio_desejada),
    CONSTRAINT ck_solicitacao_distancia CHECK (distancia_km IS NULL OR distancia_km >= 0),
    CONSTRAINT ck_solicitacao_justificativa CHECK (
        status NOT IN ('REJEITADA', 'CANCELADA')
        OR (justificativa IS NOT NULL AND btrim(justificativa) <> '')),
    CONSTRAINT ck_solicitacao_analise CHECK (
        status NOT IN ('APROVADA', 'REJEITADA')
        OR analisado_por_id IS NOT NULL)
);

CREATE INDEX ix_solicitacao_cliente ON solicitacao (cliente_id);
CREATE INDEX ix_solicitacao_status ON solicitacao (status);

-- ---------------------------------------------------------------------
-- ITEM_SOLICITACAO (modelos e quantidades pedidos)
-- ---------------------------------------------------------------------
CREATE TABLE item_solicitacao (
    id              BIGINT GENERATED ALWAYS AS IDENTITY,
    solicitacao_id  BIGINT  NOT NULL,
    modelo_id       BIGINT  NOT NULL,
    quantidade      INTEGER NOT NULL,
    CONSTRAINT pk_item_solicitacao PRIMARY KEY (id),
    CONSTRAINT uk_item_sol_solicitacao_modelo UNIQUE (solicitacao_id, modelo_id),
    CONSTRAINT fk_item_sol_solicitacao FOREIGN KEY (solicitacao_id)
        REFERENCES solicitacao (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_item_sol_modelo FOREIGN KEY (modelo_id)
        REFERENCES modelo_equipamento (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT ck_item_sol_quantidade CHECK (quantidade > 0)
);

CREATE INDEX ix_item_solicitacao_modelo ON item_solicitacao (modelo_id);

-- ---------------------------------------------------------------------
-- CONTRATO: liga o contrato a solicitacao de origem e guarda a
-- justificativa de cancelamento (RD012).
-- solicitacao_id fica nulo no banco (contratos de teste do V1 nao
-- quebram a migration); a aplicacao sempre o preenche.
-- ---------------------------------------------------------------------
ALTER TABLE contrato ADD COLUMN solicitacao_id BIGINT;
ALTER TABLE contrato ADD COLUMN justificativa_cancelamento TEXT;

ALTER TABLE contrato ADD CONSTRAINT uk_contrato_solicitacao UNIQUE (solicitacao_id);
ALTER TABLE contrato ADD CONSTRAINT fk_contrato_solicitacao FOREIGN KEY (solicitacao_id)
    REFERENCES solicitacao (id) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE contrato ADD CONSTRAINT ck_contrato_justificativa_cancelamento CHECK (
    status <> 'CANCELADO'
    OR (justificativa_cancelamento IS NOT NULL AND btrim(justificativa_cancelamento) <> ''));

-- ---------------------------------------------------------------------
-- DEVOLUCAO (retorno fisico de uma unidade; 1:1 com item_contrato)
-- ---------------------------------------------------------------------
CREATE TABLE devolucao (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY,
    item_contrato_id      BIGINT       NOT NULL,
    usuario_recebedor_id  BIGINT       NOT NULL,
    data_devolucao        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    observacoes           TEXT,
    CONSTRAINT pk_devolucao PRIMARY KEY (id),
    CONSTRAINT uk_devolucao_item_contrato UNIQUE (item_contrato_id),
    CONSTRAINT fk_devolucao_item_contrato FOREIGN KEY (item_contrato_id)
        REFERENCES item_contrato (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_devolucao_recebedor FOREIGN KEY (usuario_recebedor_id)
        REFERENCES usuario (id) ON UPDATE RESTRICT ON DELETE RESTRICT
);

CREATE INDEX ix_devolucao_recebedor ON devolucao (usuario_recebedor_id);

-- ---------------------------------------------------------------------
-- VISTORIA (inspecao tecnica apos a devolucao)
-- Laudo obrigatorio quando reprovada (RD022).
-- ---------------------------------------------------------------------
CREATE TABLE vistoria (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    devolucao_id        BIGINT        NOT NULL,
    usuario_tecnico_id  BIGINT        NOT NULL,
    data_vistoria       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    status              VARCHAR(20)   NOT NULL,
    custo_dano          NUMERIC(10,2) NOT NULL DEFAULT 0,
    laudo               TEXT,
    CONSTRAINT pk_vistoria PRIMARY KEY (id),
    CONSTRAINT uk_vistoria_devolucao UNIQUE (devolucao_id),
    CONSTRAINT fk_vistoria_devolucao FOREIGN KEY (devolucao_id)
        REFERENCES devolucao (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_vistoria_tecnico FOREIGN KEY (usuario_tecnico_id)
        REFERENCES usuario (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT ck_vistoria_status CHECK (status IN ('APROVADA', 'REPROVADA_COM_DANOS')),
    CONSTRAINT ck_vistoria_custo CHECK (custo_dano >= 0),
    CONSTRAINT ck_vistoria_laudo CHECK (
        status = 'APROVADA'
        OR (laudo IS NOT NULL AND btrim(laudo) <> ''))
);

CREATE INDEX ix_vistoria_tecnico ON vistoria (usuario_tecnico_id);

-- ---------------------------------------------------------------------
-- MANUTENCAO (conserto de unidades)
-- vistoria_id nulo = manutencao avulsa (tecnico escolhe a unidade pelo
-- numero de serie).
-- ---------------------------------------------------------------------
CREATE TABLE manutencao (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    unidade_id          BIGINT        NOT NULL,
    vistoria_id         BIGINT,
    usuario_tecnico_id  BIGINT        NOT NULL,
    data_inicio         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    data_fim            TIMESTAMPTZ,
    descricao_problema  TEXT          NOT NULL,
    custo_reparo        NUMERIC(10,2) NOT NULL DEFAULT 0,
    status              VARCHAR(20)   NOT NULL DEFAULT 'EM_ANDAMENTO',
    CONSTRAINT pk_manutencao PRIMARY KEY (id),
    CONSTRAINT fk_manutencao_unidade FOREIGN KEY (unidade_id)
        REFERENCES unidade_equipamento (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_manutencao_vistoria FOREIGN KEY (vistoria_id)
        REFERENCES vistoria (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_manutencao_tecnico FOREIGN KEY (usuario_tecnico_id)
        REFERENCES usuario (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT ck_manutencao_status CHECK (status IN (
        'AGUARDANDO', 'EM_ANDAMENTO', 'CONCLUIDA')),
    CONSTRAINT ck_manutencao_periodo CHECK (data_fim IS NULL OR data_fim >= data_inicio),
    CONSTRAINT ck_manutencao_custo CHECK (custo_reparo >= 0),
    CONSTRAINT ck_manutencao_conclusao CHECK (status <> 'CONCLUIDA' OR data_fim IS NOT NULL)
);

CREATE INDEX ix_manutencao_unidade_status ON manutencao (unidade_id, status);

-- ---------------------------------------------------------------------
-- FATURA (uma por contrato)
-- Gerada na devolucao da ultima unidade, com o contrato ainda ATIVO.
-- valor_danos e recalculado pela aplicacao enquanto a fatura estiver
-- PENDENTE (vistorias podem terminar depois); fatura PAGA nao muda.
-- VENCIDA e atualizada por rotina agendada idempotente.
-- ---------------------------------------------------------------------
CREATE TABLE fatura (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY,
    contrato_id           BIGINT        NOT NULL,
    usuario_pagamento_id  BIGINT,
    data_emissao          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    data_vencimento       DATE          NOT NULL,
    data_pagamento        TIMESTAMPTZ,
    valor_base            NUMERIC(10,2) NOT NULL,
    valor_multa_atraso    NUMERIC(10,2) NOT NULL DEFAULT 0,
    valor_danos           NUMERIC(10,2) NOT NULL DEFAULT 0,
    valor_total           NUMERIC(10,2) NOT NULL,
    status                VARCHAR(20)   NOT NULL DEFAULT 'PENDENTE',
    CONSTRAINT pk_fatura PRIMARY KEY (id),
    CONSTRAINT uk_fatura_contrato UNIQUE (contrato_id),
    CONSTRAINT fk_fatura_contrato FOREIGN KEY (contrato_id)
        REFERENCES contrato (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_fatura_usuario_pagamento FOREIGN KEY (usuario_pagamento_id)
        REFERENCES usuario (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT ck_fatura_status CHECK (status IN (
        'PENDENTE', 'PAGA', 'CANCELADA', 'VENCIDA')),
    CONSTRAINT ck_fatura_valores CHECK (
        valor_base >= 0
        AND valor_multa_atraso >= 0
        AND valor_danos >= 0
        AND valor_total = (valor_base + valor_multa_atraso + valor_danos)),
    CONSTRAINT ck_fatura_pagamento CHECK (
        status <> 'PAGA'
        OR (data_pagamento IS NOT NULL AND usuario_pagamento_id IS NOT NULL))
);

CREATE INDEX ix_fatura_status_vencimento ON fatura (status, data_vencimento);