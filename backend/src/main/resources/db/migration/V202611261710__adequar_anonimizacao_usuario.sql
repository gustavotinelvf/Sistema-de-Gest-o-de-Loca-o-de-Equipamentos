-- Registra a data de anonimizacao do usuario.
ALTER TABLE usuario
    ADD COLUMN anonimizado_em timestamptz;

-- Exige CPF para gerentes ativos.
-- Preserva registros anteriores que ainda precisam ser regularizados.
ALTER TABLE usuario
    ADD CONSTRAINT usuario_gerente_cpf_check
    CHECK (
        perfil <> 'GERENTE'
        OR status <> 'ATIVO'
        OR cpf IS NOT NULL
    ) NOT VALID;

-- Usuarios anonimizados devem permanecer inativos,
-- sem CPF e sem credenciais de acesso.
ALTER TABLE usuario
    ADD CONSTRAINT usuario_anonimizacao_check
    CHECK (
        anonimizado_em IS NULL
        OR (
            status = 'INATIVO'
            AND cpf IS NULL
            AND senha_hash IS NULL
        )
    );