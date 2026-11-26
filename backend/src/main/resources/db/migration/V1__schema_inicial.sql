CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE locadora (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    razao_social varchar(200) NOT NULL CHECK (btrim(razao_social) <> ''),
    cnpj varchar(14) NOT NULL UNIQUE CHECK (cnpj ~ '^[0-9]{14}$'),
    cpf_responsavel varchar(11) CHECK (cpf_responsavel ~ '^[0-9]{11}$'),
    nome_responsavel varchar(200),
    cep_sede varchar(8) NOT NULL CHECK (cep_sede ~ '^[0-9]{8}$'),
    logradouro_sede varchar(200) NOT NULL,
    numero_sede varchar(20) NOT NULL,
    complemento_sede varchar(100),
    bairro_sede varchar(100) NOT NULL,
    cidade_sede varchar(100) NOT NULL,
    uf_sede varchar(2) NOT NULL CHECK (uf_sede IN
        ('AC','AL','AP','AM','BA','CE','DF','ES','GO','MA','MT','MS','MG',
         'PA','PB','PR','PE','PI','RJ','RN','RS','RO','RR','SC','SP','SE','TO')),
    raio_entrega_km numeric(8,2) NOT NULL CHECK (raio_entrega_km > 0),
    percentual_multa_atraso numeric(5,2) CHECK (percentual_multa_atraso BETWEEN 0 AND 100),
    ativo boolean NOT NULL DEFAULT true,
    data_cadastro timestamptz NOT NULL DEFAULT current_timestamp
);

CREATE TABLE usuario (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    locadora_id bigint NOT NULL REFERENCES locadora(id),
    nome varchar(200) NOT NULL CHECK (btrim(nome) <> ''),
    email varchar(200) NOT NULL CHECK (email ~ '^[^[:space:]@]+@[^[:space:]@]+\.[^[:space:]@]+$'),
    cpf varchar(11) UNIQUE CHECK (cpf ~ '^[0-9]{11}$'),
    senha_hash varchar(255),
    perfil varchar(30) NOT NULL CHECK (perfil IN
        ('RESPONSAVEL','GERENTE','ATENDENTE','ESTOQUISTA','TECNICO','FINANCEIRO')),
    cargo varchar(100),
    status varchar(15) NOT NULL DEFAULT 'PENDENTE'
        CHECK (status IN ('PENDENTE','ATIVO','INATIVO')),
    data_cadastro timestamptz NOT NULL DEFAULT current_timestamp,
    data_ativacao timestamptz,
    UNIQUE (id, locadora_id),
    CHECK (status <> 'ATIVO' OR (senha_hash IS NOT NULL AND data_ativacao IS NOT NULL)),
    CHECK (senha_hash IS NULL OR senha_hash ~ '^\$2[aby]\$[0-9]{2}\$[./A-Za-z0-9]{53}$')
);
CREATE UNIQUE INDEX usuario_email_unico ON usuario(lower(email));
CREATE INDEX usuario_locadora_idx ON usuario(locadora_id);

CREATE TABLE convite (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id bigint NOT NULL REFERENCES usuario(id),
    token_hash varchar(64) NOT NULL UNIQUE CHECK (token_hash ~ '^[0-9a-f]{64}$'),
    status varchar(15) NOT NULL DEFAULT 'PENDENTE'
        CHECK (status IN ('PENDENTE','ACEITO','EXPIRADO','CANCELADO')),
    data_envio timestamptz NOT NULL DEFAULT current_timestamp,
    data_expiracao timestamptz NOT NULL,
    data_aceite timestamptz,
    CHECK (data_expiracao > data_envio),
    CHECK ((status = 'ACEITO') = (data_aceite IS NOT NULL)),
    CHECK (data_aceite IS NULL OR (data_aceite >= data_envio AND data_aceite < data_expiracao))
);
CREATE UNIQUE INDEX convite_pendente_unico ON convite(usuario_id) WHERE status = 'PENDENTE';

CREATE TABLE cliente (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    locadora_id bigint NOT NULL REFERENCES locadora(id),
    razao_social varchar(200) NOT NULL CHECK (btrim(razao_social) <> ''),
    cnpj varchar(14) NOT NULL UNIQUE CHECK (cnpj ~ '^[0-9]{14}$'),
    representante_nome varchar(200),
    representante_cpf varchar(11) CHECK (representante_cpf ~ '^[0-9]{11}$'),
    email varchar(200),
    telefone varchar(20),
    cep varchar(8) NOT NULL CHECK (cep ~ '^[0-9]{8}$'),
    logradouro varchar(200) NOT NULL,
    numero varchar(20) NOT NULL,
    complemento varchar(100),
    bairro varchar(100) NOT NULL,
    cidade varchar(100) NOT NULL,
    uf varchar(2) NOT NULL CHECK (uf IN
        ('AC','AL','AP','AM','BA','CE','DF','ES','GO','MA','MT','MS','MG',
         'PA','PB','PR','PE','PI','RJ','RN','RS','RO','RR','SC','SP','SE','TO')),
    status_adimplencia varchar(15) NOT NULL DEFAULT 'ADIMPLENTE'
        CHECK (status_adimplencia IN ('ADIMPLENTE','INADIMPLENTE')),
    ativo boolean NOT NULL DEFAULT true,
    data_cadastro timestamptz NOT NULL DEFAULT current_timestamp,
    data_anonimizacao timestamptz,
    UNIQUE (id, locadora_id)
);
CREATE INDEX cliente_locadora_idx ON cliente(locadora_id);

CREATE TABLE modelo_equipamento (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    locadora_id bigint NOT NULL REFERENCES locadora(id),
    nome varchar(200) NOT NULL CHECK (btrim(nome) <> ''),
    categoria varchar(100) NOT NULL,
    especificacoes text NOT NULL,
    valor_diaria numeric(12,2) NOT NULL CHECK (valor_diaria > 0),
    quantidade_minima integer NOT NULL DEFAULT 0 CHECK (quantidade_minima >= 0),
    quantidade_maxima integer CHECK (quantidade_maxima > quantidade_minima),
    ativo boolean NOT NULL DEFAULT true,
    UNIQUE (id, locadora_id)
);
CREATE INDEX modelo_categoria_idx ON modelo_equipamento(locadora_id, categoria);

CREATE TABLE unidade_equipamento (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    locadora_id bigint NOT NULL REFERENCES locadora(id),
    modelo_id bigint NOT NULL,
    numero_serie varchar(50) NOT NULL CHECK (btrim(numero_serie) <> ''),
    status varchar(20) NOT NULL DEFAULT 'DISPONIVEL'
        CHECK (status IN ('DISPONIVEL','LOCADA','EM_VISTORIA','EM_MANUTENCAO','INDISPONIVEL')),
    ativo boolean NOT NULL DEFAULT true,
    UNIQUE (locadora_id, numero_serie),
    UNIQUE (id, locadora_id),
    UNIQUE (id, modelo_id, locadora_id),
    FOREIGN KEY (modelo_id, locadora_id) REFERENCES modelo_equipamento(id, locadora_id)
);
CREATE INDEX unidade_modelo_status_idx ON unidade_equipamento(modelo_id, status);

CREATE TABLE solicitacao (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    locadora_id bigint NOT NULL REFERENCES locadora(id),
    cliente_id bigint NOT NULL,
    solicitante_id bigint NOT NULL,
    aprovador_id bigint,
    data_inicio_prevista date NOT NULL,
    data_fim_prevista date NOT NULL,
    valor_estimado numeric(12,2) NOT NULL CHECK (valor_estimado >= 0),
    distancia_entrega_km numeric(8,2) CHECK (distancia_entrega_km >= 0),
    status varchar(15) NOT NULL DEFAULT 'PENDENTE'
        CHECK (status IN ('PENDENTE','APROVADA','REJEITADA','CANCELADA')),
    justificativa text,
    data_solicitacao timestamptz NOT NULL DEFAULT current_timestamp,
    data_analise timestamptz,
    UNIQUE (id, locadora_id),
    UNIQUE (id, cliente_id, locadora_id),
    FOREIGN KEY (cliente_id, locadora_id) REFERENCES cliente(id, locadora_id),
    FOREIGN KEY (solicitante_id, locadora_id) REFERENCES usuario(id, locadora_id),
    FOREIGN KEY (aprovador_id, locadora_id) REFERENCES usuario(id, locadora_id),
    CHECK (data_fim_prevista > data_inicio_prevista),
    CHECK (status NOT IN ('REJEITADA','CANCELADA') OR nullif(btrim(justificativa), '') IS NOT NULL),
    CHECK (status NOT IN ('APROVADA','REJEITADA') OR (aprovador_id IS NOT NULL AND data_analise IS NOT NULL))
);
CREATE INDEX solicitacao_cliente_status_idx ON solicitacao(cliente_id, status);

CREATE TABLE item_solicitacao (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    locadora_id bigint NOT NULL REFERENCES locadora(id),
    solicitacao_id bigint NOT NULL,
    modelo_id bigint NOT NULL,
    quantidade integer NOT NULL CHECK (quantidade > 0),
    valor_unitario numeric(12,2) NOT NULL CHECK (valor_unitario > 0),
    valor_total numeric(14,2) GENERATED ALWAYS AS (quantidade * valor_unitario) STORED,
    UNIQUE (solicitacao_id, modelo_id),
    UNIQUE (id, solicitacao_id, modelo_id, locadora_id),
    FOREIGN KEY (solicitacao_id, locadora_id) REFERENCES solicitacao(id, locadora_id),
    FOREIGN KEY (modelo_id, locadora_id) REFERENCES modelo_equipamento(id, locadora_id)
);

CREATE TABLE contrato (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    locadora_id bigint NOT NULL REFERENCES locadora(id),
    solicitacao_id bigint NOT NULL UNIQUE,
    cliente_id bigint NOT NULL,
    responsavel_id bigint NOT NULL,
    data_inicio_prevista date NOT NULL,
    data_fim_prevista date NOT NULL,
    valor_locacao numeric(12,2) NOT NULL CHECK (valor_locacao > 0),
    valor_entrega numeric(12,2) NOT NULL DEFAULT 0 CHECK (valor_entrega >= 0),
    distancia_entrega_km numeric(8,2) CHECK (distancia_entrega_km >= 0),
    percentual_multa_atraso numeric(5,2) NOT NULL CHECK (percentual_multa_atraso BETWEEN 0 AND 100),
    condicoes_devolucao text NOT NULL CHECK (btrim(condicoes_devolucao) <> ''),
    status varchar(15) NOT NULL DEFAULT 'PLANEJADO'
        CHECK (status IN ('PLANEJADO','ATIVO','FINALIZADO','CANCELADO')),
    justificativa_cancelamento text,
    data_formalizacao timestamptz NOT NULL DEFAULT current_timestamp,
    data_retirada timestamptz,
    responsavel_retirada_id bigint,
    UNIQUE (id, locadora_id),
    UNIQUE (id, solicitacao_id, locadora_id),
    UNIQUE (id, data_inicio_prevista, data_fim_prevista, status),
    FOREIGN KEY (solicitacao_id, cliente_id, locadora_id)
        REFERENCES solicitacao(id, cliente_id, locadora_id),
    FOREIGN KEY (responsavel_id, locadora_id) REFERENCES usuario(id, locadora_id),
    FOREIGN KEY (responsavel_retirada_id, locadora_id) REFERENCES usuario(id, locadora_id),
    CHECK (data_fim_prevista > data_inicio_prevista),
    CHECK (status <> 'CANCELADO' OR nullif(btrim(justificativa_cancelamento), '') IS NOT NULL),
    CHECK (status NOT IN ('ATIVO','FINALIZADO') OR
        (data_retirada IS NOT NULL AND responsavel_retirada_id IS NOT NULL))
);
CREATE INDEX contrato_cliente_status_idx ON contrato(cliente_id, status);

CREATE TABLE item_contrato (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    locadora_id bigint NOT NULL REFERENCES locadora(id),
    contrato_id bigint NOT NULL,
    solicitacao_id bigint NOT NULL,
    item_solicitacao_id bigint NOT NULL,
    modelo_id bigint NOT NULL,
    unidade_id bigint NOT NULL,
    valor_diaria numeric(12,2) NOT NULL CHECK (valor_diaria > 0),
    data_inicio_prevista date NOT NULL,
    data_fim_prevista date NOT NULL,
    status_contrato varchar(15) NOT NULL DEFAULT 'PLANEJADO',
    UNIQUE (contrato_id, unidade_id),
    UNIQUE (id, locadora_id),
    FOREIGN KEY (contrato_id, solicitacao_id, locadora_id)
        REFERENCES contrato(id, solicitacao_id, locadora_id),
    FOREIGN KEY (item_solicitacao_id, solicitacao_id, modelo_id, locadora_id)
        REFERENCES item_solicitacao(id, solicitacao_id, modelo_id, locadora_id),
    FOREIGN KEY (unidade_id, modelo_id, locadora_id)
        REFERENCES unidade_equipamento(id, modelo_id, locadora_id),
    FOREIGN KEY (contrato_id, data_inicio_prevista, data_fim_prevista, status_contrato)
        REFERENCES contrato(id, data_inicio_prevista, data_fim_prevista, status)
        ON UPDATE CASCADE,
    CHECK (data_fim_prevista > data_inicio_prevista),
    -- Datas inclusivas: devolucao prevista e nova retirada no mesmo dia conflitam.
    CONSTRAINT reserva_sem_sobreposicao EXCLUDE USING gist (
        unidade_id WITH =,
        daterange(data_inicio_prevista, data_fim_prevista, '[]') WITH &&
    ) WHERE (status_contrato IN ('PLANEJADO','ATIVO'))
);
CREATE INDEX item_contrato_periodo_idx ON item_contrato(unidade_id, data_inicio_prevista, data_fim_prevista);
CREATE INDEX item_contrato_contrato_idx ON item_contrato(contrato_id);

CREATE TABLE devolucao (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    locadora_id bigint NOT NULL REFERENCES locadora(id),
    item_contrato_id bigint NOT NULL UNIQUE,
    usuario_id bigint NOT NULL,
    data_devolucao_real timestamptz NOT NULL DEFAULT current_timestamp,
    multa_atraso numeric(12,2) NOT NULL DEFAULT 0 CHECK (multa_atraso >= 0),
    observacoes text,
    UNIQUE (id, locadora_id),
    FOREIGN KEY (item_contrato_id, locadora_id) REFERENCES item_contrato(id, locadora_id),
    FOREIGN KEY (usuario_id, locadora_id) REFERENCES usuario(id, locadora_id)
);

CREATE TABLE vistoria (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    locadora_id bigint NOT NULL REFERENCES locadora(id),
    devolucao_id bigint NOT NULL UNIQUE,
    tecnico_id bigint NOT NULL,
    data_inicio timestamptz NOT NULL DEFAULT current_timestamp,
    data_conclusao timestamptz,
    resultado varchar(10) CHECK (resultado IN ('APROVADA','REPROVADA')),
    descricao_avarias text,
    FOREIGN KEY (devolucao_id, locadora_id) REFERENCES devolucao(id, locadora_id),
    FOREIGN KEY (tecnico_id, locadora_id) REFERENCES usuario(id, locadora_id),
    CHECK ((data_conclusao IS NULL) = (resultado IS NULL)),
    CHECK (data_conclusao IS NULL OR data_conclusao >= data_inicio),
    CHECK (resultado <> 'REPROVADA' OR nullif(btrim(descricao_avarias), '') IS NOT NULL)
);

CREATE TABLE manutencao (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    locadora_id bigint NOT NULL REFERENCES locadora(id),
    unidade_id bigint NOT NULL,
    tecnico_id bigint NOT NULL,
    tipo varchar(15) NOT NULL CHECK (tipo IN ('PREVENTIVA','CORRETIVA')),
    descricao text NOT NULL CHECK (btrim(descricao) <> ''),
    custo_gerado numeric(12,2) NOT NULL DEFAULT 0 CHECK (custo_gerado >= 0),
    data_inicio timestamptz NOT NULL DEFAULT current_timestamp,
    data_prevista timestamptz,
    data_conclusao timestamptz,
    justificativa_atraso text,
    FOREIGN KEY (unidade_id, locadora_id) REFERENCES unidade_equipamento(id, locadora_id),
    FOREIGN KEY (tecnico_id, locadora_id) REFERENCES usuario(id, locadora_id),
    CHECK (data_prevista IS NULL OR data_prevista >= data_inicio),
    CHECK (data_conclusao IS NULL OR data_conclusao >= data_inicio),
    CHECK (data_conclusao IS NULL OR data_prevista IS NULL OR data_conclusao <= data_prevista
        OR nullif(btrim(justificativa_atraso), '') IS NOT NULL)
);
CREATE UNIQUE INDEX manutencao_aberta_unica ON manutencao(unidade_id) WHERE data_conclusao IS NULL;

CREATE TABLE fatura (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    locadora_id bigint NOT NULL REFERENCES locadora(id),
    contrato_id bigint NOT NULL UNIQUE,
    valor_locacao numeric(12,2) NOT NULL CHECK (valor_locacao > 0),
    valor_multa numeric(12,2) NOT NULL DEFAULT 0 CHECK (valor_multa BETWEEN 0 AND valor_locacao),
    valor_total numeric(13,2) GENERATED ALWAYS AS (valor_locacao + valor_multa) STORED,
    status varchar(15) NOT NULL DEFAULT 'PENDENTE'
        CHECK (status IN ('PENDENTE','PAGA','CANCELADA','VENCIDA')),
    data_emissao date NOT NULL DEFAULT current_date,
    data_vencimento date NOT NULL,
    data_pagamento date,
    responsavel_emissao_id bigint NOT NULL,
    responsavel_pagamento_id bigint,
    FOREIGN KEY (contrato_id, locadora_id) REFERENCES contrato(id, locadora_id),
    FOREIGN KEY (responsavel_emissao_id, locadora_id) REFERENCES usuario(id, locadora_id),
    FOREIGN KEY (responsavel_pagamento_id, locadora_id) REFERENCES usuario(id, locadora_id),
    CHECK (data_vencimento >= data_emissao),
    CHECK ((status = 'PAGA') = (data_pagamento IS NOT NULL AND responsavel_pagamento_id IS NOT NULL)),
    CHECK (status = 'PAGA' OR (data_pagamento IS NULL AND responsavel_pagamento_id IS NULL)),
    CHECK (data_pagamento IS NULL OR data_pagamento >= data_emissao)
);
CREATE INDEX fatura_status_vencimento_idx ON fatura(status, data_vencimento);

COMMENT ON TABLE locadora IS 'Empresa que opera o sistema; distinta das empresas clientes.';
COMMENT ON TABLE usuario IS 'Usuarios da aplicacao. Nao sao usuarios de conexao PostgreSQL.';
COMMENT ON TABLE item_contrato IS 'Uma linha por unidade fisica; reserva protegida por EXCLUDE GiST.';
COMMENT ON COLUMN contrato.valor_locacao IS 'Valor total contratado, incluindo entrega. Copia historica.';
COMMENT ON COLUMN contrato.percentual_multa_atraso IS 'Copia do percentual na formalizacao; formula por atraso no backend.';
COMMENT ON TABLE fatura IS 'Uma fatura por contrato. Valores historicos, nao FKs para valores monetarios.';
