-- =====================================================================
-- ZipLoc | V202611221900__item_contrato_valor_diaria_contratada.sql
-- PostgreSQL 16 | Gerenciado exclusivamente pelo Flyway
-- Escopo: preservacao da diaria contratada por item do contrato.
-- O esquema oficial ja contempla essa funcionalidade em valor_diaria.
-- =====================================================================

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'item_contrato'
          AND column_name = 'valor_diaria'
    ) THEN
        RAISE EXCEPTION 'Coluna valor_diaria ausente no esquema oficial.';
    END IF;
END;
$$;