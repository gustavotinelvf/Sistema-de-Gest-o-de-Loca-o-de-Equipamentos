CREATE FUNCTION validar_solicitacao() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE c cliente%ROWTYPE;
BEGIN
    IF TG_OP = 'INSERT' OR NEW.cliente_id <> OLD.cliente_id OR
       (NEW.status = 'APROVADA' AND OLD.status <> 'APROVADA') THEN
        SELECT * INTO STRICT c FROM cliente WHERE id = NEW.cliente_id FOR SHARE;
        IF NOT c.ativo OR c.status_adimplencia = 'INADIMPLENTE' THEN
            RAISE EXCEPTION 'Cliente inativo ou inadimplente nao pode solicitar/aprovar locacao';
        END IF;
    END IF;
    IF TG_OP = 'INSERT' AND NEW.status <> 'PENDENTE' THEN
        RAISE EXCEPTION 'Solicitacao deve nascer PENDENTE';
    END IF;
    IF TG_OP = 'UPDATE' AND OLD.status <> 'PENDENTE' AND NEW IS DISTINCT FROM OLD THEN
        RAISE EXCEPTION 'Solicitacao ja analisada nao pode ser alterada';
    END IF;
    IF NEW.status IN ('APROVADA','REJEITADA') THEN
        IF NOT EXISTS (SELECT 1 FROM usuario WHERE id = NEW.aprovador_id
            AND status = 'ATIVO' AND perfil IN ('RESPONSAVEL','GERENTE')) THEN
            RAISE EXCEPTION 'Aprovador deve ser gerente ou responsavel ativo';
        END IF;
        IF NEW.status = 'APROVADA' AND NOT EXISTS (
            SELECT 1 FROM item_solicitacao WHERE solicitacao_id = NEW.id) THEN
            RAISE EXCEPTION 'Solicitacao aprovada precisa de itens';
        END IF;
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER solicitacao_validar BEFORE INSERT OR UPDATE ON solicitacao
FOR EACH ROW EXECUTE FUNCTION validar_solicitacao();

CREATE FUNCTION proteger_item_solicitacao() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE sid bigint;
BEGIN
    IF TG_OP = 'UPDATE' AND (NEW.solicitacao_id, NEW.locadora_id) IS DISTINCT FROM
        (OLD.solicitacao_id, OLD.locadora_id) THEN
        RAISE EXCEPTION 'Nao e permitido transferir item de solicitacao';
    END IF;
    IF TG_OP = 'DELETE' THEN sid := OLD.solicitacao_id; ELSE sid := NEW.solicitacao_id; END IF;
    PERFORM 1 FROM solicitacao WHERE id = sid AND status = 'PENDENTE' FOR UPDATE;
    IF NOT FOUND THEN RAISE EXCEPTION 'Itens so podem mudar em solicitacao PENDENTE'; END IF;
    IF TG_OP = 'DELETE' THEN RETURN OLD; END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER item_solicitacao_proteger BEFORE INSERT OR UPDATE OR DELETE ON item_solicitacao
FOR EACH ROW EXECUTE FUNCTION proteger_item_solicitacao();

CREATE FUNCTION validar_contrato() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE raio numeric;
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'Contrato preservado para historico; utilize cancelamento';
    END IF;
    IF TG_OP = 'INSERT' THEN
        IF NEW.status <> 'PLANEJADO' THEN RAISE EXCEPTION 'Contrato deve nascer PLANEJADO'; END IF;
        PERFORM 1 FROM solicitacao WHERE id = NEW.solicitacao_id AND status = 'APROVADA' FOR SHARE;
        IF NOT FOUND THEN RAISE EXCEPTION 'Contrato exige solicitacao APROVADA'; END IF;
        IF NOT EXISTS (SELECT 1 FROM usuario WHERE id = NEW.responsavel_id
            AND status = 'ATIVO' AND perfil IN ('RESPONSAVEL','GERENTE')) THEN
            RAISE EXCEPTION 'Formalizacao exige gerente ou responsavel ativo';
        END IF;
    ELSE
        IF (NEW.solicitacao_id, NEW.cliente_id, NEW.locadora_id, NEW.percentual_multa_atraso)
            IS DISTINCT FROM
           (OLD.solicitacao_id, OLD.cliente_id, OLD.locadora_id, OLD.percentual_multa_atraso) THEN
            RAISE EXCEPTION 'Partes, solicitacao e percentual sao historicos imutaveis';
        END IF;
        IF OLD.status IN ('FINALIZADO','CANCELADO') AND NEW IS DISTINCT FROM OLD THEN
            RAISE EXCEPTION 'Contrato encerrado nao pode ser alterado';
        END IF;
        IF NEW.status <> OLD.status AND NOT (
            (OLD.status = 'PLANEJADO' AND NEW.status IN ('ATIVO','CANCELADO')) OR
            (OLD.status = 'ATIVO' AND NEW.status = 'FINALIZADO')) THEN
            RAISE EXCEPTION 'Transicao de contrato invalida: % -> %', OLD.status, NEW.status;
        END IF;
        IF NEW.status = 'ATIVO' AND OLD.status = 'PLANEJADO' THEN
            IF NOT EXISTS (SELECT 1 FROM item_contrato WHERE contrato_id = NEW.id) OR EXISTS (
                SELECT 1 FROM item_solicitacao s
                WHERE s.solicitacao_id = NEW.solicitacao_id
                AND s.quantidade <> (SELECT count(*) FROM item_contrato i
                                    WHERE i.contrato_id = NEW.id AND i.item_solicitacao_id = s.id)) THEN
                RAISE EXCEPTION 'Retirada exige alocacao completa das quantidades solicitadas';
            END IF;
            PERFORM u.id FROM unidade_equipamento u JOIN item_contrato i ON i.unidade_id = u.id
                WHERE i.contrato_id = NEW.id ORDER BY u.id FOR UPDATE OF u;
            IF EXISTS (SELECT 1 FROM unidade_equipamento u
                JOIN item_contrato i ON i.unidade_id = u.id
                JOIN modelo_equipamento m ON m.id = u.modelo_id
                WHERE i.contrato_id = NEW.id AND
                  (NOT u.ativo OR NOT m.ativo OR u.status <> 'DISPONIVEL')) THEN
                RAISE EXCEPTION 'Unidade nao esta disponivel para retirada';
            END IF;
        END IF;
        IF NEW.status = 'FINALIZADO' AND OLD.status <> 'FINALIZADO' THEN
            IF NOT EXISTS (SELECT 1 FROM fatura WHERE contrato_id = NEW.id AND status = 'PAGA')
               OR NOT EXISTS (SELECT 1 FROM item_contrato WHERE contrato_id = NEW.id)
               OR EXISTS (SELECT 1 FROM item_contrato i WHERE i.contrato_id = NEW.id
                    AND NOT EXISTS (SELECT 1 FROM devolucao d WHERE d.item_contrato_id = i.id)) THEN
                RAISE EXCEPTION 'Finalizacao exige todas as devolucoes e fatura PAGA';
            END IF;
        END IF;
        IF (NEW.valor_locacao, NEW.valor_entrega) IS DISTINCT FROM (OLD.valor_locacao, OLD.valor_entrega)
           AND EXISTS (SELECT 1 FROM fatura WHERE contrato_id = NEW.id) THEN
            RAISE EXCEPTION 'Valores nao podem mudar apos emissao da fatura';
        END IF;
        IF (NEW.data_inicio_prevista, NEW.data_fim_prevista) IS DISTINCT FROM
           (OLD.data_inicio_prevista, OLD.data_fim_prevista) THEN
            IF EXISTS (SELECT 1 FROM item_contrato i JOIN devolucao d ON d.item_contrato_id = i.id
                       WHERE i.contrato_id = NEW.id) THEN
                RAISE EXCEPTION 'Periodo nao pode mudar apos devolucao';
            END IF;
        END IF;
    END IF;
    SELECT raio_entrega_km INTO raio FROM locadora WHERE id = NEW.locadora_id;
    IF NEW.distancia_entrega_km > raio THEN RAISE EXCEPTION 'Distancia excede raio de entrega'; END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER contrato_validar BEFORE INSERT OR UPDATE OR DELETE ON contrato
FOR EACH ROW EXECUTE FUNCTION validar_contrato();

CREATE FUNCTION registrar_retirada() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.status = 'ATIVO' AND OLD.status = 'PLANEJADO' THEN
        UPDATE unidade_equipamento SET status = 'LOCADA'
        WHERE id IN (SELECT unidade_id FROM item_contrato WHERE contrato_id = NEW.id);
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER contrato_retirada AFTER UPDATE ON contrato
FOR EACH ROW EXECUTE FUNCTION registrar_retirada();

CREATE FUNCTION validar_reserva() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE c contrato%ROWTYPE; u unidade_equipamento%ROWTYPE; qtd integer;
BEGIN
    IF TG_OP = 'UPDATE' AND
       (NEW.contrato_id, NEW.solicitacao_id, NEW.item_solicitacao_id, NEW.modelo_id, NEW.unidade_id, NEW.locadora_id)
       IS DISTINCT FROM
       (OLD.contrato_id, OLD.solicitacao_id, OLD.item_solicitacao_id, OLD.modelo_id, OLD.unidade_id, OLD.locadora_id) THEN
        RAISE EXCEPTION 'Nao e permitido transferir uma alocacao; remova e recrie enquanto PLANEJADO';
    END IF;
    IF TG_OP = 'DELETE' THEN
        SELECT * INTO STRICT c FROM contrato WHERE id = OLD.contrato_id FOR UPDATE;
        IF c.status <> 'PLANEJADO' THEN RAISE EXCEPTION 'Item de contrato em andamento e historico'; END IF;
        RETURN OLD;
    END IF;
    SELECT * INTO STRICT c FROM contrato WHERE id = NEW.contrato_id FOR UPDATE;
    -- Datas e status nunca sao editados diretamente: acompanham o contrato pai.
    NEW.data_inicio_prevista := c.data_inicio_prevista;
    NEW.data_fim_prevista := c.data_fim_prevista;
    NEW.status_contrato := c.status;
    IF TG_OP = 'INSERT' THEN
        IF c.status <> 'PLANEJADO' THEN RAISE EXCEPTION 'Alocacao exige contrato PLANEJADO'; END IF;
        SELECT * INTO STRICT u FROM unidade_equipamento WHERE id = NEW.unidade_id FOR UPDATE;
        IF NOT u.ativo OR u.status <> 'DISPONIVEL' OR NOT EXISTS (
            SELECT 1 FROM modelo_equipamento WHERE id = u.modelo_id AND ativo) THEN
            RAISE EXCEPTION 'Unidade inativa ou indisponivel para reserva';
        END IF;
        SELECT quantidade INTO STRICT qtd FROM item_solicitacao WHERE id = NEW.item_solicitacao_id;
        IF (SELECT count(*) FROM item_contrato WHERE contrato_id = NEW.contrato_id
            AND item_solicitacao_id = NEW.item_solicitacao_id) >= qtd THEN
            RAISE EXCEPTION 'Alocacao excede quantidade solicitada';
        END IF;
    ELSIF c.status IN ('FINALIZADO','CANCELADO') AND NEW.valor_diaria <> OLD.valor_diaria THEN
        RAISE EXCEPTION 'Valor de item encerrado e historico';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER item_contrato_validar BEFORE INSERT OR UPDATE OR DELETE ON item_contrato
FOR EACH ROW EXECUTE FUNCTION validar_reserva();

CREATE FUNCTION validar_convite() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'INSERT' AND NEW.status <> 'PENDENTE' THEN
        RAISE EXCEPTION 'Convite deve nascer PENDENTE';
    END IF;
    IF TG_OP = 'UPDATE' AND OLD.status <> 'PENDENTE' AND NEW IS DISTINCT FROM OLD THEN
        RAISE EXCEPTION 'Convite encerrado nao pode ser alterado';
    END IF;
    IF NEW.status = 'ACEITO' AND clock_timestamp() >= NEW.data_expiracao THEN
        RAISE EXCEPTION 'Convite expirado nao pode ser aceito';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER convite_validar BEFORE INSERT OR UPDATE ON convite
FOR EACH ROW EXECUTE FUNCTION validar_convite();

CREATE FUNCTION registrar_devolucao() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE i item_contrato%ROWTYPE; c contrato%ROWTYPE;
BEGIN
    SELECT * INTO STRICT i FROM item_contrato WHERE id = NEW.item_contrato_id;
    SELECT * INTO STRICT c FROM contrato WHERE id = i.contrato_id FOR UPDATE;
    IF c.status <> 'ATIVO' THEN RAISE EXCEPTION 'Devolucao exige contrato ATIVO'; END IF;
    IF NEW.data_devolucao_real < c.data_retirada THEN
        RAISE EXCEPTION 'Devolucao anterior a retirada';
    END IF;
    IF NEW.multa_atraso + COALESCE((SELECT sum(d.multa_atraso) FROM devolucao d
        JOIN item_contrato ic ON ic.id = d.item_contrato_id WHERE ic.contrato_id = c.id), 0) > c.valor_locacao THEN
        RAISE EXCEPTION 'Soma de multas excede valor do contrato';
    END IF;
    PERFORM 1 FROM unidade_equipamento WHERE id = i.unidade_id FOR UPDATE;
    UPDATE unidade_equipamento SET status = 'EM_VISTORIA' WHERE id = i.unidade_id;
    IF (NEW.data_devolucao_real AT TIME ZONE 'America/Sao_Paulo')::date > c.data_fim_prevista THEN
        UPDATE cliente SET status_adimplencia = 'INADIMPLENTE' WHERE id = c.cliente_id;
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER devolucao_registrar BEFORE INSERT ON devolucao
FOR EACH ROW EXECUTE FUNCTION registrar_devolucao();

CREATE FUNCTION impedir_alteracao_historico() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'Registro de % preservado para historico; use ajuste por migration revisada', TG_TABLE_NAME;
END $$;
CREATE TRIGGER devolucao_historico BEFORE UPDATE OR DELETE ON devolucao
FOR EACH ROW EXECUTE FUNCTION impedir_alteracao_historico();

CREATE FUNCTION registrar_vistoria() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE uid bigint; devolvida_em timestamptz;
BEGIN
    IF TG_OP = 'UPDATE' AND (OLD.data_conclusao IS NOT NULL OR
        (NEW.devolucao_id, NEW.locadora_id) IS DISTINCT FROM (OLD.devolucao_id, OLD.locadora_id)) THEN
        RAISE EXCEPTION 'Vistoria concluida ou vinculo de devolucao nao pode mudar';
    END IF;
    SELECT i.unidade_id, d.data_devolucao_real INTO STRICT uid, devolvida_em
        FROM devolucao d JOIN item_contrato i ON i.id = d.item_contrato_id WHERE d.id = NEW.devolucao_id;
    IF NEW.data_inicio < devolvida_em THEN RAISE EXCEPTION 'Vistoria anterior a devolucao'; END IF;
    PERFORM 1 FROM unidade_equipamento WHERE id = uid FOR UPDATE;
    IF NEW.resultado IS NOT NULL THEN
        IF EXISTS (SELECT 1 FROM manutencao WHERE unidade_id = uid AND data_conclusao IS NULL) THEN
            RAISE EXCEPTION 'Conclua a vistoria antes de abrir manutencao';
        END IF;
        UPDATE unidade_equipamento SET status = CASE NEW.resultado
            WHEN 'APROVADA' THEN 'DISPONIVEL' ELSE 'EM_MANUTENCAO' END WHERE id = uid;
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER vistoria_registrar BEFORE INSERT OR UPDATE ON vistoria
FOR EACH ROW EXECUTE FUNCTION registrar_vistoria();
CREATE TRIGGER vistoria_historico BEFORE DELETE ON vistoria
FOR EACH ROW EXECUTE FUNCTION impedir_alteracao_historico();

CREATE FUNCTION registrar_manutencao() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE u unidade_equipamento%ROWTYPE;
BEGIN
    IF TG_OP = 'UPDATE' AND (OLD.data_conclusao IS NOT NULL OR
        (NEW.unidade_id, NEW.locadora_id) IS DISTINCT FROM (OLD.unidade_id, OLD.locadora_id)) THEN
        RAISE EXCEPTION 'Manutencao concluida ou unidade vinculada nao pode mudar';
    END IF;
    SELECT * INTO STRICT u FROM unidade_equipamento WHERE id = NEW.unidade_id FOR UPDATE;
    IF TG_OP = 'INSERT' THEN
        IF u.status IN ('LOCADA','EM_VISTORIA') THEN
            RAISE EXCEPTION 'Unidade locada ou em vistoria nao pode entrar em manutencao';
        END IF;
        IF EXISTS (SELECT 1 FROM item_contrato WHERE unidade_id = NEW.unidade_id
            AND status_contrato IN ('PLANEJADO','ATIVO')
            AND data_fim_prevista >= (NEW.data_inicio AT TIME ZONE 'America/Sao_Paulo')::date) THEN
            RAISE EXCEPTION 'Resolva as reservas futuras antes de iniciar manutencao';
        END IF;
    END IF;
    UPDATE unidade_equipamento SET status = CASE WHEN NEW.data_conclusao IS NULL
        THEN 'EM_MANUTENCAO' ELSE 'DISPONIVEL' END WHERE id = NEW.unidade_id;
    RETURN NEW;
END $$;
CREATE TRIGGER manutencao_registrar BEFORE INSERT OR UPDATE ON manutencao
FOR EACH ROW EXECUTE FUNCTION registrar_manutencao();
CREATE TRIGGER manutencao_historico BEFORE DELETE ON manutencao
FOR EACH ROW EXECUTE FUNCTION impedir_alteracao_historico();

CREATE FUNCTION validar_fatura() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE c contrato%ROWTYPE; multa numeric;
BEGIN
    IF TG_OP = 'UPDATE' THEN
        IF OLD.status IN ('PAGA','CANCELADA') AND NEW IS DISTINCT FROM OLD THEN
            RAISE EXCEPTION 'Fatura encerrada nao pode mudar';
        END IF;
        IF (NEW.contrato_id, NEW.locadora_id) IS DISTINCT FROM (OLD.contrato_id, OLD.locadora_id) THEN
            RAISE EXCEPTION 'Nao e permitido transferir fatura';
        END IF;
    END IF;
    SELECT * INTO STRICT c FROM contrato WHERE id = NEW.contrato_id FOR UPDATE;
    IF TG_OP = 'INSERT' THEN
        IF NEW.status <> 'PENDENTE' THEN RAISE EXCEPTION 'Fatura deve nascer PENDENTE'; END IF;
        IF c.status <> 'ATIVO' OR NOT EXISTS (SELECT 1 FROM item_contrato WHERE contrato_id = c.id)
           OR EXISTS (SELECT 1 FROM item_contrato i WHERE i.contrato_id = c.id
               AND NOT EXISTS (SELECT 1 FROM devolucao d WHERE d.item_contrato_id = i.id)) THEN
            RAISE EXCEPTION 'Emissao exige contrato ATIVO com todas as unidades devolvidas';
        END IF;
    END IF;
    SELECT COALESCE(sum(d.multa_atraso), 0) INTO multa FROM devolucao d
        JOIN item_contrato i ON i.id = d.item_contrato_id WHERE i.contrato_id = c.id;
    IF NEW.valor_locacao <> c.valor_locacao OR NEW.valor_multa <> LEAST(multa, c.valor_locacao) THEN
        RAISE EXCEPTION 'Fatura deve refletir o valor contratado e as multas registradas';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER fatura_validar BEFORE INSERT OR UPDATE ON fatura
FOR EACH ROW EXECUTE FUNCTION validar_fatura();
CREATE TRIGGER fatura_historico BEFORE DELETE ON fatura
FOR EACH ROW EXECUTE FUNCTION impedir_alteracao_historico();

CREATE VIEW vw_estoque_modelo AS
SELECT m.id AS modelo_id, m.locadora_id, m.nome, m.categoria,
    count(u.id) FILTER (WHERE u.ativo) AS quantidade_total,
    count(u.id) FILTER (WHERE u.ativo AND u.status = 'DISPONIVEL') AS quantidade_disponivel_agora,
    count(u.id) FILTER (WHERE u.ativo AND u.status = 'LOCADA') AS quantidade_locada,
    count(u.id) FILTER (WHERE u.ativo AND u.status = 'EM_VISTORIA') AS quantidade_em_vistoria,
    count(u.id) FILTER (WHERE u.ativo AND u.status = 'EM_MANUTENCAO') AS quantidade_em_manutencao
FROM modelo_equipamento m LEFT JOIN unidade_equipamento u ON u.modelo_id = m.id
GROUP BY m.id;

CREATE FUNCTION consultar_disponibilidade(p_locadora_id bigint, p_modelo_id bigint, p_inicio date, p_fim date)
RETURNS TABLE (unidade_id bigint, numero_serie varchar, valor_diaria numeric)
LANGUAGE plpgsql STABLE AS $$
BEGIN
    IF p_inicio IS NULL OR p_fim IS NULL OR p_fim <= p_inicio THEN
        RAISE EXCEPTION 'Informe periodo com fim posterior ao inicio';
    END IF;
    RETURN QUERY SELECT u.id, u.numero_serie, m.valor_diaria
    FROM unidade_equipamento u JOIN modelo_equipamento m ON m.id = u.modelo_id
    WHERE u.locadora_id = p_locadora_id AND m.id = p_modelo_id
      AND u.ativo AND m.ativo AND u.status = 'DISPONIVEL'
      AND NOT EXISTS (SELECT 1 FROM item_contrato i WHERE i.unidade_id = u.id
          AND i.status_contrato IN ('PLANEJADO','ATIVO')
          AND daterange(i.data_inicio_prevista, i.data_fim_prevista, '[]') && daterange(p_inicio, p_fim, '[]'))
    ORDER BY u.id;
END $$;

CREATE VIEW vw_faturas_cliente AS
SELECT f.*, c.cliente_id, cli.razao_social AS cliente_razao_social,
    (f.status IN ('PENDENTE','VENCIDA') AND f.data_vencimento < current_date) AS vencida_por_data
FROM fatura f JOIN contrato c ON c.id = f.contrato_id JOIN cliente cli ON cli.id = c.cliente_id;

CREATE VIEW vw_vistorias_pendentes AS
SELECT d.id AS devolucao_id, d.locadora_id, i.unidade_id, v.id AS vistoria_id,
    d.data_devolucao_real, d.data_devolucao_real + interval '48 hours' AS prazo_vistoria,
    current_timestamp > d.data_devolucao_real + interval '48 hours' AS prazo_excedido
FROM devolucao d JOIN item_contrato i ON i.id = d.item_contrato_id
LEFT JOIN vistoria v ON v.devolucao_id = d.id
WHERE v.data_conclusao IS NULL;
