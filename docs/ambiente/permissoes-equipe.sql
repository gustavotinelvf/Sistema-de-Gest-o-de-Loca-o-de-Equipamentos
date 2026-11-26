DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'ziploc_equipe') THEN
        CREATE ROLE ziploc_equipe NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE;
    END IF;
END $$;
GRANT CONNECT ON DATABASE ziploc TO ziploc_equipe;
GRANT USAGE ON SCHEMA public TO ziploc_equipe;
REVOKE CREATE ON SCHEMA public FROM PUBLIC;

GRANT SELECT, INSERT, UPDATE, DELETE ON
    locadora, usuario, convite, cliente, modelo_equipamento, unidade_equipamento,
    solicitacao, item_solicitacao, contrato, item_contrato, devolucao, vistoria, manutencao, fatura
TO ziploc_equipe;
GRANT SELECT ON vw_estoque_modelo, vw_faturas_cliente, vw_vistorias_pendentes TO ziploc_equipe;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO ziploc_equipe;