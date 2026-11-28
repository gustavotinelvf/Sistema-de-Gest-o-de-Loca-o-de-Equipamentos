package br.ufs.ziploc.vistoria;

import br.ufs.ziploc.manutencao.domain.TipoManutencao;
import br.ufs.ziploc.manutencao.service.ManutencaoService;
import br.ufs.ziploc.shared.exception.RecursoNaoEncontradoException;
import br.ufs.ziploc.vistoria.domain.ResultadoVistoria;
import br.ufs.ziploc.vistoria.service.VistoriaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "ziploc.lgpd.cron=-")
@Transactional
class VistoriaIntegracaoTest {

    @Autowired
    private VistoriaService service;

    @Autowired
    private ManutencaoService manutencaoService;

    @Autowired
    private JdbcTemplate jdbc;

    private long locadora;
    private long cliente;
    private long gerente;
    private long tecnico;
    private long modelo;
    private long unidade;
    private long devolucao;
    private String numeroSerie;
    private LocalDate hoje;

    @BeforeEach
    void preparar() {
        String sufixo = UUID.randomUUID().toString();

        hoje = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        numeroSerie = "VIS-" + sufixo;

        locadora = jdbc.queryForObject("""
                INSERT INTO locadora (
                    razao_social, cnpj, cep_sede, logradouro_sede,
                    numero_sede, bairro_sede, cidade_sede, uf_sede,
                    raio_entrega_km, percentual_multa_atraso
                )
                VALUES (
                    'Locadora Vistoria', ?, '49000000',
                    'Rua A', '1', 'Centro', 'Aracaju', 'SE', 100, 10
                )
                RETURNING id
                """, Long.class, cnpj());

        cliente = jdbc.queryForObject("""
                INSERT INTO cliente (
                    locadora_id, razao_social, cnpj,
                    cep, logradouro, numero, bairro, cidade, uf
                )
                VALUES (
                    ?, 'Cliente Vistoria', ?,
                    '49000000', 'Rua A', '1', 'Centro', 'Aracaju', 'SE'
                )
                RETURNING id
                """, Long.class, locadora, cnpj());

        gerente = criarUsuario("GERENTE", sufixo);
        tecnico = criarUsuario("TECNICO", sufixo);

        modelo = jdbc.queryForObject("""
                INSERT INTO modelo_equipamento (
                    locadora_id, nome, categoria,
                    especificacoes, valor_diaria
                )
                VALUES (?, 'Furadeira', 'Ferramentas', 'Teste', 50)
                RETURNING id
                """, Long.class, locadora);

        unidade = jdbc.queryForObject("""
                INSERT INTO unidade_equipamento (
                    locadora_id, modelo_id, numero_serie
                )
                VALUES (?, ?, ?)
                RETURNING id
                """, Long.class, locadora, modelo, numeroSerie);
    }

    @Test
    void aprovadaLiberaUnidadeSemAbrirManutencao() {
        criarDevolucao(false);

        var resultado = service.registrar(
                locadora, devolucao,
                ResultadoVistoria.APROVADA,
                null, tecnico);

        assertThat(resultado.id()).isNotNull();
        assertThat(resultado.resultado())
                .isEqualTo(ResultadoVistoria.APROVADA);
        assertThat(resultado.tecnicoId()).isEqualTo(tecnico);
        assertThat(resultado.dataConclusao()).isNotNull();

        assertThat(statusUnidade()).isEqualTo("DISPONIVEL");
        assertThat(contarVistorias()).isEqualTo(1L);
        assertThat(contarManutencoesAbertas()).isZero();

        assertThat(jdbc.queryForObject("""
                SELECT resultado FROM vistoria WHERE devolucao_id = ?
                """, String.class, devolucao))
                .isEqualTo("APROVADA");
    }

    @Test
    void reprovadaAbreManutencaoCorretiva() {
        criarDevolucao(false);

        var resultado = service.registrar(
                locadora, devolucao,
                ResultadoVistoria.REPROVADA,
                "Motor danificado", tecnico);

        assertThat(resultado.id()).isNotNull();
        assertThat(resultado.resultado())
                .isEqualTo(ResultadoVistoria.REPROVADA);

        verificarManutencaoCorretiva();
    }

    @Test
    void reprovadaComReservaFuturaPriorizaManutencao() {
        long contratoFuturo = criarDevolucao(true);

        var resultado = service.registrar(
                locadora, devolucao,
                ResultadoVistoria.REPROVADA,
                "Motor danificado", tecnico);

        assertThat(resultado.id()).isNotNull();

        verificarManutencaoCorretiva();

        assertThat(jdbc.queryForObject("""
                SELECT status FROM contrato WHERE id = ?
                """, String.class, contratoFuturo))
                .isEqualTo("PLANEJADO");

        assertThat(jdbc.queryForObject("""
                SELECT unidade_id FROM item_contrato
                WHERE contrato_id = ?
                """, Long.class, contratoFuturo))
                .isEqualTo(unidade);

        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM vw_reservas_afetadas_manutencao
                WHERE locadora_id = ?
                  AND contrato_id = ?
                  AND unidade_id = ?
                """, Long.class, locadora, contratoFuturo, unidade))
                .isEqualTo(1L);

        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM consultar_disponibilidade(?, ?, ?, ?)
                """, Long.class, locadora, modelo,
                hoje.plusDays(15), hoje.plusDays(17)))
                .isZero();
    }

    @Test
    void impedeRetiradaDeUnidadeEmManutencao() {
        long contratoFuturo = criarDevolucao(true);

        service.registrar(
                locadora, devolucao,
                ResultadoVistoria.REPROVADA,
                "Motor danificado", tecnico);

        assertThat(statusUnidade()).isEqualTo("EM_MANUTENCAO");

        assertThatThrownBy(() -> jdbc.update("""
                UPDATE contrato
                SET status = 'ATIVO',
                    data_retirada = now(),
                    responsavel_retirada_id = ?
                WHERE id = ?
                """, gerente, contratoFuturo))
                .isInstanceOf(DataAccessException.class)
                .hasStackTraceContaining(
                        "Unidade nao esta disponivel para retirada");
    }

    @Test
    void manutencaoPreventivaComReservaContinuaBloqueada() {
        criarContrato(
                hoje.plusDays(3),
                hoje.plusDays(5));

        assertThat(statusUnidade()).isEqualTo("DISPONIVEL");

        assertThatThrownBy(() -> manutencaoService.iniciarAvulsa(
                locadora,
                numeroSerie,
                TipoManutencao.PREVENTIVA,
                "Revisão preventiva",
                null,
                tecnico))
                .isInstanceOf(RuntimeException.class)
                .hasStackTraceContaining(
                        "Resolva as reservas futuras antes de iniciar manutencao preventiva");
    }

    @Test
    void manutencaoCorretivaAvulsaTambemTemPrioridade() {
        long contratoFuturo = criarContrato(
                hoje.plusDays(3),
                hoje.plusDays(5));

        var manutencao = manutencaoService.iniciarAvulsa(
                locadora,
                numeroSerie,
                TipoManutencao.CORRETIVA,
                "Defeito identificado",
                null,
                tecnico);

        assertThat(manutencao.id()).isNotNull();
        assertThat(statusUnidade()).isEqualTo("EM_MANUTENCAO");

        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM vw_reservas_afetadas_manutencao
                WHERE contrato_id = ?
                """, Long.class, contratoFuturo))
                .isEqualTo(1L);

        manutencaoService.concluir(
                manutencao.id(),
                new BigDecimal("120.00"),
                null);

        assertThat(statusUnidade()).isEqualTo("DISPONIVEL");

        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM vw_reservas_afetadas_manutencao
                WHERE contrato_id = ?
                """, Long.class, contratoFuturo))
                .isZero();

        assertThat(jdbc.queryForObject("""
                SELECT status FROM contrato WHERE id = ?
                """, String.class, contratoFuturo))
                .isEqualTo("PLANEJADO");
    }

    @Test
    void naoPermiteVistoriaDuplicada() {
        criarDevolucao(false);

        service.registrar(
                locadora, devolucao,
                ResultadoVistoria.REPROVADA,
                "Motor danificado", tecnico);

        assertThatThrownBy(() -> service.registrar(
                locadora, devolucao,
                ResultadoVistoria.REPROVADA,
                "Outra descrição", tecnico))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("já foi concluída");

        assertThat(contarVistorias()).isEqualTo(1L);
        assertThat(contarManutencoesAbertas()).isEqualTo(1L);
    }

    @Test
    void naoPermiteVistoriaDeOutraLocadora() {
        criarDevolucao(false);

        assertThatThrownBy(() -> service.registrar(
                locadora + 1, devolucao,
                ResultadoVistoria.APROVADA,
                null, tecnico))
                .isInstanceOf(RecursoNaoEncontradoException.class);

        assertThat(contarVistorias()).isZero();
        assertThat(statusUnidade()).isEqualTo("EM_VISTORIA");
    }

    @Test
    void reprovacaoSemAvariasNaoAlteraBanco() {
        criarDevolucao(false);

        assertThatThrownBy(() -> service.registrar(
                locadora, devolucao,
                ResultadoVistoria.REPROVADA,
                "  ", tecnico))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("RD022");

        assertThat(contarVistorias()).isZero();
        assertThat(contarManutencoesAbertas()).isZero();
        assertThat(statusUnidade()).isEqualTo("EM_VISTORIA");
    }

    @Test
    void concluiVistoriaPendentePreservandoIdentidade() {
        criarDevolucao(false);

        long vistoriaId = jdbc.queryForObject("""
                INSERT INTO vistoria (
                    locadora_id, devolucao_id,
                    tecnico_id, data_inicio
                )
                VALUES (?, ?, ?, clock_timestamp())
                RETURNING id
                """, Long.class, locadora, devolucao, tecnico);

        var resultado = service.registrar(
                locadora, devolucao,
                ResultadoVistoria.APROVADA,
                null, tecnico);

        assertThat(resultado.id()).isEqualTo(vistoriaId);
        assertThat(contarVistorias()).isEqualTo(1L);
        assertThat(statusUnidade()).isEqualTo("DISPONIVEL");
    }

    @Test
    void equipePossuiPermissaoDeConsultaDaNovaView() {
        assertThat(jdbc.queryForObject("""
                SELECT has_table_privilege(
                    'ziploc_equipe',
                    'public.vw_reservas_afetadas_manutencao',
                    'SELECT'
                )
                """, Boolean.class))
                .isTrue();
    }

    private long criarDevolucao(boolean comReservaFutura) {
        long contrato = criarContrato(
                hoje.minusDays(9), hoje);

        long reserva = comReservaFutura
                ? criarContrato(
                        hoje.plusDays(3),
                        hoje.plusDays(5))
                : 0L;

        jdbc.update("""
                UPDATE contrato
                SET status = 'ATIVO',
                    data_retirada = now(),
                    responsavel_retirada_id = ?
                WHERE id = ?
                """, gerente, contrato);

        long item = jdbc.queryForObject("""
                SELECT id FROM item_contrato
                WHERE contrato_id = ?
                """, Long.class, contrato);

        devolucao = jdbc.queryForObject("""
                INSERT INTO devolucao (
                    locadora_id, item_contrato_id,
                    usuario_id, data_devolucao_real, multa_atraso
                )
                VALUES (?, ?, ?, now(), 0)
                RETURNING id
                """, Long.class, locadora, item, gerente);

        assertThat(statusUnidade()).isEqualTo("EM_VISTORIA");

        return reserva;
    }

    private long criarContrato(LocalDate inicio, LocalDate fim) {
        long solicitacao = jdbc.queryForObject("""
                INSERT INTO solicitacao (
                    locadora_id, cliente_id, solicitante_id,
                    data_inicio_prevista, data_fim_prevista,
                    valor_estimado
                )
                VALUES (?, ?, ?, ?, ?, 500)
                RETURNING id
                """, Long.class, locadora, cliente,
                gerente, inicio, fim);

        long itemSolicitacao = jdbc.queryForObject("""
                INSERT INTO item_solicitacao (
                    locadora_id, solicitacao_id,
                    modelo_id, quantidade, valor_unitario
                )
                VALUES (?, ?, ?, 1, 50)
                RETURNING id
                """, Long.class, locadora, solicitacao, modelo);

        jdbc.update("""
                UPDATE solicitacao
                SET status = 'APROVADA',
                    aprovador_id = ?,
                    data_analise = now()
                WHERE id = ?
                """, gerente, solicitacao);

        long contrato = jdbc.queryForObject("""
                INSERT INTO contrato (
                    locadora_id, solicitacao_id, cliente_id,
                    responsavel_id, data_inicio_prevista,
                    data_fim_prevista, valor_locacao,
                    percentual_multa_atraso, condicoes_devolucao
                )
                VALUES (?, ?, ?, ?, ?, ?, 500, 10, 'Devolver no prazo')
                RETURNING id
                """, Long.class, locadora, solicitacao,
                cliente, gerente, inicio, fim);

        jdbc.update("""
                INSERT INTO item_contrato (
                    locadora_id, contrato_id, solicitacao_id,
                    item_solicitacao_id, modelo_id, unidade_id,
                    valor_diaria, data_inicio_prevista, data_fim_prevista
                )
                VALUES (?, ?, ?, ?, ?, ?, 50, ?, ?)
                """, locadora, contrato, solicitacao,
                itemSolicitacao, modelo, unidade, inicio, fim);

        return contrato;
    }

    private void verificarManutencaoCorretiva() {
        assertThat(statusUnidade()).isEqualTo("EM_MANUTENCAO");
        assertThat(contarVistorias()).isEqualTo(1L);
        assertThat(contarManutencoesAbertas()).isEqualTo(1L);

        assertThat(jdbc.queryForObject("""
                SELECT tipo FROM manutencao
                WHERE unidade_id = ? AND data_conclusao IS NULL
                """, String.class, unidade))
                .isEqualTo("CORRETIVA");

        assertThat(jdbc.queryForObject("""
                SELECT descricao FROM manutencao
                WHERE unidade_id = ? AND data_conclusao IS NULL
                """, String.class, unidade))
                .isEqualTo("Motor danificado");

        assertThat(jdbc.queryForObject("""
                SELECT tecnico_id FROM manutencao
                WHERE unidade_id = ? AND data_conclusao IS NULL
                """, Long.class, unidade))
                .isEqualTo(tecnico);
    }

    private long criarUsuario(String perfil, String sufixo) {
        return jdbc.queryForObject("""
                INSERT INTO usuario (
                    locadora_id, nome, email, cpf,
                    senha_hash, perfil, status, data_ativacao
                )
                VALUES (?, ?, ?, ?, ?, ?, 'ATIVO', now())
                RETURNING id
                """, Long.class, locadora, perfil,
                perfil.toLowerCase() + "." + sufixo + "@teste.com",
                "GERENTE".equals(perfil) ? cpf() : null,
                "$2a$10$" + "A".repeat(53),
                perfil);
    }

    private String statusUnidade() {
        return jdbc.queryForObject("""
                SELECT status FROM unidade_equipamento WHERE id = ?
                """, String.class, unidade);
    }

    private long contarVistorias() {
        return jdbc.queryForObject("""
                SELECT COUNT(*) FROM vistoria WHERE devolucao_id = ?
                """, Long.class, devolucao);
    }

    private long contarManutencoesAbertas() {
        return jdbc.queryForObject("""
                SELECT COUNT(*) FROM manutencao
                WHERE unidade_id = ? AND data_conclusao IS NULL
                """, Long.class, unidade);
    }

    private static String cnpj() {
        return String.format("%014d",
                Math.floorMod(
                        UUID.randomUUID().getMostSignificantBits(),
                        100_000_000_000_000L));
    }

    private static String cpf() {
        return String.format("%011d",
                Math.floorMod(
                        UUID.randomUUID().getMostSignificantBits(),
                        100_000_000_000L));
    }
}