-- Registra o inicio do periodo de retencao dos usuarios inativos.
ALTER TABLE usuario
    ADD COLUMN data_inativacao timestamptz;

-- Para usuarios anteriormente inativados, inicia um novo periodo
-- de retencao, pois a data real da inativacao nao e conhecida.
UPDATE usuario
SET data_inativacao = CURRENT_TIMESTAMP
WHERE status = 'INATIVO';

-- Garante consistencia entre o status e a data de inativacao.
ALTER TABLE usuario
    ADD CONSTRAINT usuario_data_inativacao_check
    CHECK (
        (status = 'INATIVO' AND data_inativacao IS NOT NULL)
        OR
        (status <> 'INATIVO' AND data_inativacao IS NULL)
    );

-- Reserva o dominio de email para usuarios anonimizados.
ALTER TABLE usuario
    ADD CONSTRAINT usuario_email_anonimizacao_check
    CHECK (
        (
            anonimizado_em IS NULL
            AND LOWER(TRIM(email)) NOT LIKE '%@anonimizado.invalid'
        )
        OR
        (
            anonimizado_em IS NOT NULL
            AND email = 'anonimizado-' || id::text || '@anonimizado.invalid'
        )
    );

-- Otimiza a busca de candidatos a anonimizacao.
CREATE INDEX idx_usuario_retencao_inativos
    ON usuario (data_inativacao)
    WHERE status = 'INATIVO'
      AND anonimizado_em IS NULL;