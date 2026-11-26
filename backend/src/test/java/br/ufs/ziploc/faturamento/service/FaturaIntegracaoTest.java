package br.ufs.ziploc.faturamento.service;

import br.ufs.ziploc.faturamento.dto.FaturaDto;
import br.ufs.ziploc.faturamento.domain.StatusFatura;
import br.ufs.ziploc.faturamento.exception.FaturaJaExisteException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class FaturaIntegracaoTest {

    @Autowired
    private FaturaService service;

    @Autowired
    private JdbcTemplate jdbc;

    private long locadora;
    private long cliente;
    private long gerente;
    private long financeiro;
    private long modelo;
    private long unidade;
    private long contrato;

    @BeforeEach
    void preparar() {
        String sufixo = UUID.randomUUID().toString();

        LocalDate inicio = LocalDate.now().minusDays(9);
        LocalDate fim = LocalDate.now();

        locadora = jdbc.queryForObject(
                "INSERT INTO locadora (razao_social, cnpj, cep_sede, logradouro_sede, numero_sede, bairro_sede, cidade_sede, uf_sede, raio_entrega_km, percentual_multa_atraso) VALUES ('Locadora Faturamento', ?, '49000000', 'Rua A', '1', 'Centro', 'Aracaju', 'SE', 100, 10) RETURNING id",
                Long.class, cnpj());

        cliente = jdbc.queryForObject(
                "INSERT INTO cliente (locadora_id, razao_social, cnpj, cep, logradouro, numero, bairro, cidade, uf) VALUES (?, 'Cliente Faturamento', ?, '49000000', 'Rua A', '1', 'Centro', 'Aracaju', 'SE') RETURNING id",
                Long.class, locadora, cnpj());

        gerente = criarUsuario("GERENTE", sufixo);
        financeiro = criarUsuario("FINANCEIRO", sufixo);

        modelo = jdbc.queryForObject(
                "INSERT INTO modelo_equipamento (locadora_id, nome, categoria, especificacoes, valor_diaria) VALUES (?, 'Andaime', 'Andaime', 'Equipamento de teste', 50) RETURNING id",
                Long.class, locadora);

        unidade = jdbc.queryForObject(
                "INSERT INTO unidade_equipamento (locadora_id, modelo_id, numero_serie) VALUES (?, ?, 'FAT-1') RETURNING id",
                Long.class, locadora, modelo);

        long solicitacao = jdbc.queryForObject(
                "INSERT INTO solicitacao (locadora_id, cliente_id, solicitante_id, data_inicio_prevista, data_fim_prevista, valor_estimado) VALUES (?, ?, ?, ?, ?, 500) RETURNING id",
                Long.class, locadora, cliente, gerente, inicio, fim);

        long itemSolicitacao = jdbc.queryForObject(
                "INSERT INTO item_solicitacao (locadora_id, solicitacao_id, modelo_id, quantidade, valor_unitario) VALUES (?, ?, ?, 1, 50) RETURNING id",
                Long.class, locadora, solicitacao, modelo);

        jdbc.update(
                "UPDATE solicitacao SET status = 'APROVADA', aprovador_id = ?, data_analise = now() WHERE id = ?",
                gerente, solicitacao);

        contrato = jdbc.queryForObject(
                "INSERT INTO contrato (locadora_id, solicitacao_id, cliente_id, responsavel_id, data_inicio_prevista, data_fim_prevista, valor_locacao, percentual_multa_atraso, condicoes_devolucao) VALUES (?, ?, ?, ?, ?, ?, 500, 10, 'Devolver no prazo') RETURNING id",
                Long.class, locadora, solicitacao, cliente, gerente, inicio, fim);

        long itemContrato = jdbc.queryForObject(
                "INSERT INTO item_contrato (locadora_id, contrato_id, solicitacao_id, item_solicitacao_id, modelo_id, unidade_id, valor_diaria, data_inicio_prevista, data_fim_prevista) VALUES (?, ?, ?, ?, ?, ?, 50, ?, ?) RETURNING id",
                Long.class, locadora, contrato, solicitacao, itemSolicitacao, modelo, unidade, inicio, fim);

        jdbc.update(
                "UPDATE contrato SET status = 'ATIVO', data_retirada = now(), responsavel_retirada_id = ? WHERE id = ?",
                gerente, contrato);

        jdbc.update(
                "INSERT INTO devolucao (locadora_id, item_contrato_id, usuario_id, data_devolucao_real, multa_atraso) VALUES (?, ?, ?, now(), 0)",
                locadora, itemContrato, gerente);
    }

    @Test
    void deveRegistrarPagamentoEFinalizarContrato() {

        long fatura = jdbc.queryForObject(
                "INSERT INTO fatura (locadora_id, contrato_id, valor_locacao, valor_multa, data_emissao, data_vencimento, responsavel_emissao_id) VALUES (?, ?, 500, 0, CURRENT_DATE, CURRENT_DATE + 7, ?) RETURNING id",
                Long.class, locadora, contrato, financeiro);

        FaturaDto resultado = service.registrarPagamento(
                fatura, locadora, financeiro);

        assertThat(resultado.status()).isEqualTo(StatusFatura.PAGA);

        assertThat(resultado.valorTotal())
                .isEqualByComparingTo(new BigDecimal("500.00"));

        assertThat(jdbc.queryForObject(
                "SELECT status FROM fatura WHERE id = ?",
                String.class, fatura))
                .isEqualTo("PAGA");

        assertThat(jdbc.queryForObject(
                "SELECT status FROM contrato WHERE id = ?",
                String.class, contrato))
                .isEqualTo("FINALIZADO");

        assertThat(jdbc.queryForObject(
                "SELECT responsavel_pagamento_id FROM fatura WHERE id = ?",
                Long.class, fatura))
                .isEqualTo(financeiro);
    }

    @Test
    void deveGerarFaturaConsolidadaNoBanco() {

        FaturaDto resultado = service.gerarFatura(
                contrato, locadora, financeiro, BigDecimal.ZERO);

        LocalDate hoje = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

        assertThat(resultado.id()).isNotNull();

        assertThat(resultado.contratoId()).isEqualTo(contrato);

        assertThat(resultado.status()).isEqualTo(StatusFatura.PENDENTE);

        assertThat(resultado.valorLocacao())
                .isEqualByComparingTo(new BigDecimal("500.00"));

        assertThat(resultado.valorMulta())
                .isEqualByComparingTo(BigDecimal.ZERO);

        assertThat(resultado.valorTotal())
                .isEqualByComparingTo(new BigDecimal("500.00"));

        assertThat(resultado.dataVencimento())
                .isEqualTo(hoje.plusDays(7));

        assertThat(jdbc.queryForObject(
                "SELECT valor_total FROM fatura WHERE id = ?",
                BigDecimal.class, resultado.id()))
                .isEqualByComparingTo(new BigDecimal("500.00"));

        assertThat(jdbc.queryForObject(
                "SELECT status FROM fatura WHERE id = ?",
                String.class, resultado.id()))
                .isEqualTo("PENDENTE");

        assertThat(jdbc.queryForObject(
                "SELECT responsavel_emissao_id FROM fatura WHERE id = ?",
                Long.class, resultado.id()))
                .isEqualTo(financeiro);

        assertThat(jdbc.queryForObject(
                "SELECT status FROM contrato WHERE id = ?",
                String.class, contrato))
                .isEqualTo("ATIVO");
    }

    @Test
    void deveImpedirEmissaoDuplicadaNoBanco() {

        service.gerarFatura(
                contrato, locadora, financeiro, BigDecimal.ZERO);

        assertThatThrownBy(() -> service.gerarFatura(
                contrato, locadora, financeiro, BigDecimal.ZERO))
                .isInstanceOf(FaturaJaExisteException.class);

        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM fatura WHERE contrato_id = ?",
                Long.class, contrato))
                .isEqualTo(1L);
    }

    private long criarUsuario(String perfil, String sufixo) {
        return jdbc.queryForObject(
                "INSERT INTO usuario (locadora_id, nome, email, senha_hash, perfil, status, data_ativacao) VALUES (?, ?, ?, ?, ?, 'ATIVO', now()) RETURNING id",
                Long.class, locadora, perfil,
                perfil.toLowerCase() + "." + sufixo + "@teste.com",
                "$2a$10$" + "A".repeat(53), perfil);
    }

    private static String cnpj() {
        return String.format("%014d",
                Math.floorMod(UUID.randomUUID().getMostSignificantBits(),
                        100_000_000_000_000L));
    }
}