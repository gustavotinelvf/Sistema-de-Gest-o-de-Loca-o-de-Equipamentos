-- =====================================================================
-- ZipLoc | V202611221900__item_contrato_valor_diaria_contratada.sql
-- PostgreSQL 16 | Gerenciado exclusivamente pelo Flyway
-- Escopo: diaria contratada por item do contrato. O valor e copiado de
--         modelo_equipamento.valor_diaria_base na aprovacao da
--         solicitacao e nao muda se o preco do modelo for alterado depois.
-- =====================================================================

ALTER TABLE item_contrato ADD COLUMN valor_diaria_contratada NUMERIC(10,2);

UPDATE item_contrato ic
   SET valor_diaria_contratada = m.valor_diaria_base
  FROM unidade_equipamento u
  JOIN modelo_equipamento m ON m.id = u.modelo_id
 WHERE u.id = ic.unidade_id;

ALTER TABLE item_contrato ALTER COLUMN valor_diaria_contratada SET NOT NULL;

ALTER TABLE item_contrato ADD CONSTRAINT ck_item_contrato_valor_diaria
    CHECK (valor_diaria_contratada > 0);
