-- Remove o perfil RESPONSAVEL, conforme os cinco perfis definidos no projeto.
-- Preserva o historico das migrations V1 e V2.

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM usuario WHERE perfil = 'RESPONSAVEL') THEN
        RAISE EXCEPTION 'Existem usuarios com perfil RESPONSAVEL. Regularize os perfis antes de aplicar esta migration.';
    END IF;
END $$;

ALTER TABLE usuario
    DROP CONSTRAINT usuario_perfil_check;

ALTER TABLE usuario
    ADD CONSTRAINT usuario_perfil_check
    CHECK (perfil IN ('GERENTE','ATENDENTE','ESTOQUISTA','TECNICO','FINANCEIRO'));

CREATE OR REPLACE FUNCTION validar_solicitacao() RETURNS trigger LANGUAGE plpgsql AS $$
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
            AND status = 'ATIVO' AND perfil = 'GERENTE') THEN
            RAISE EXCEPTION 'Aprovador deve ser gerente ativo';
        END IF;
        IF NEW.status = 'APROVADA' AND NOT EXISTS (
            SELECT 1 FROM item_solicitacao WHERE solicitacao_id = NEW.id) THEN
            RAISE EXCEPTION 'Solicitacao aprovada precisa de itens';
        END IF;
    END IF;
    RETURN NEW;
END $$;

CREATE OR REPLACE FUNCTION validar_contrato() RETURNS trigger LANGUAGE plpgsql AS $$
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
            AND status = 'ATIVO' AND perfil = 'GERENTE') THEN
            RAISE EXCEPTION 'Formalizacao exige gerente ativo';
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