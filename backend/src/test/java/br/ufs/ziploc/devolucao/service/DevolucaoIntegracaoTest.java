package br.ufs.ziploc.devolucao.service;

import br.ufs.ziploc.cliente.domain.DecisaoReconsideracao;
import br.ufs.ziploc.cliente.domain.StatusAdimplencia;
import br.ufs.ziploc.cliente.dto.RegistrarReconsideracaoDTO;
import br.ufs.ziploc.cliente.dto.ReconsideracaoResponseDTO;
import br.ufs.ziploc.cliente.service.ReconsideracaoService;
import br.ufs.ziploc.devolucao.dto.DevolucaoDto;
import br.ufs.ziploc.devolucao.dto.RegistrarDevolucaoDto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "ziploc.lgpd.cron=-"
})
@Transactional
class DevolucaoIntegracaoTest {

    private static final ZoneId FUSO =
            ZoneId.of("America/Sao_Paulo");

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DevolucaoService devolucaoService;

    @Autowired
    private ReconsideracaoService reconsideracaoService;

    private Long locadoraId;
    private Long clienteId;
    private Long gerenteId;
    private Long atendenteId;
    private Long modeloId;

    private LocalDate hoje;

    private record ContratoTeste(
            Long id,
            List<Long> itens
    ) {
    }

    @BeforeEach
    void preparar() {

        // Protege bancos que nao foram destinados aos testes.
        assertEquals(
                "ziploc_lgpd_concorrencia",
                jdbc.queryForObject(
                        "SELECT current_database()",
                        String.class
                )
        );

        hoje = LocalDate.now(FUSO);

        locadoraId = inserir("""
                INSERT INTO locadora (
                    razao_social,
                    cnpj,
                    cep_sede,
                    logradouro_sede,
                    numero_sede,
                    bairro_sede,
                    cidade_sede,
                    uf_sede,
                    raio_entrega_km,
                    percentual_multa_atraso
                )
                VALUES (
                    'Locadora Teste Devolucao',
                    ?,
                    '49000000',
                    'Rua A',
                    '1',
                    'Centro',
                    'Aracaju',
                    'SE',
                    100,
                    10
                )
                RETURNING id
                """,
                cnpj()
        );

        clienteId = inserir("""
                INSERT INTO cliente (
                    locadora_id,
                    razao_social,
                    cnpj,
                    cep,
                    logradouro,
                    numero,
                    bairro,
                    cidade,
                    uf
                )
                VALUES (
                    ?,
                    'Cliente Teste Devolucao',
                    ?,
                    '49000000',
                    'Rua B',
                    '2',
                    'Centro',
                    'Aracaju',
                    'SE'
                )
                RETURNING id
                """,
                locadoraId,
                cnpj()
        );

        gerenteId = criarUsuario("GERENTE");

        atendenteId = criarUsuario("ATENDENTE");

        modeloId = inserir("""
                INSERT INTO modelo_equipamento (
                    locadora_id,
                    nome,
                    categoria,
                    especificacoes,
                    valor_diaria
                )
                VALUES (
                    ?,
                    'Equipamento Teste',
                    'Ferramentas',
                    'Equipamento de integracao',
                    100.00
                )
                RETURNING id
                """,
                locadoraId
        );
    }

    @Test
    void deveContabilizarApenasUmaOcorrenciaPorContrato() {

        ContratoTeste contrato =
                criarContrato(5, 3);

        // Primeiro equipamento atrasado.
        DevolucaoDto primeira =
                devolver(contrato.itens().get(0));

        // Esta dentro da carencia de multa.
        assertEquals(
                new BigDecimal("0.00"),
                primeira.multaAtraso()
        );

        assertEquals(1L, ocorrenciasAtuais());

        assertEquals("ADIMPLENTE", statusCliente());

        // Outros quatro equipamentos do mesmo contrato.
        for (int i = 1; i < contrato.itens().size(); i++) {

            devolver(contrato.itens().get(i));

            assertEquals(1L, ocorrenciasAtuais());

            assertEquals("ADIMPLENTE", statusCliente());
        }

        assertEquals(1L, quantidadeFaturas(contrato.id()));
    }

    @Test
    void deveTornarClienteInadimplenteNoSegundoContratoAtrasado() {

        ContratoTeste primeiro =
                criarContrato(1, 2);

        ContratoTeste segundo =
                criarContrato(1, 3);

        devolver(primeiro.itens().getFirst());

        assertEquals(1L, ocorrenciasAtuais());

        assertEquals("ADIMPLENTE", statusCliente());

        devolver(segundo.itens().getFirst());

        assertEquals(2L, ocorrenciasAtuais());

        assertEquals("INADIMPLENTE", statusCliente());
    }

    @Test
    void naoDeveRegistrarOcorrenciaParaDevolucaoNoPrazo() {

        ContratoTeste contrato =
                criarContrato(1, 0);

        DevolucaoDto resultado =
                devolver(contrato.itens().getFirst());

        assertEquals(
                new BigDecimal("0.00"),
                resultado.multaAtraso()
        );

        assertEquals(0L, ocorrenciasAtuais());

        assertEquals("ADIMPLENTE", statusCliente());
    }

    @Test
    void deveGerarFaturaSomenteAposUltimaDevolucao() {

        ContratoTeste contrato =
                criarContrato(5, 10);

        for (int i = 0; i < 4; i++) {

            DevolucaoDto resultado =
                    devolver(contrato.itens().get(i));

            assertEquals(
                    new BigDecimal("30.00"),
                    resultado.multaAtraso()
            );

            assertEquals(
                    0L,
                    quantidadeFaturas(contrato.id())
            );
        }

        devolver(contrato.itens().get(4));

        assertEquals(
                1L,
                quantidadeFaturas(contrato.id())
        );

        BigDecimal multaTotal = jdbc.queryForObject(
                """
                SELECT SUM(d.multa_atraso)
                FROM devolucao d
                JOIN item_contrato i
                    ON i.id = d.item_contrato_id
                WHERE i.contrato_id = ?
                """,
                BigDecimal.class,
                contrato.id()
        );

        assertEquals(
                new BigDecimal("150.00"),
                multaTotal
        );

        assertEquals(1L, ocorrenciasAtuais());
    }

    @Test
    void deveColocarEquipamentoEmVistoriaAposDevolucao() {

        ContratoTeste contrato =
                criarContrato(1, 0);

        Long itemId = contrato.itens().getFirst();

        devolver(itemId);

        String status = jdbc.queryForObject(
                """
                SELECT u.status
                FROM unidade_equipamento u
                JOIN item_contrato i
                    ON i.unidade_id = u.id
                WHERE i.id = ?
                """,
                String.class,
                itemId
        );

        assertEquals("EM_VISTORIA", status);
    }

    @Test
    void deveReiniciarCicloSemApagarHistorico() {

        tornarClienteInadimplente();

        assertEquals(1, cicloAtual());

        assertEquals(2L, totalOcorrencias());

        ReconsideracaoResponseDTO resultado =
                reconsideracaoService.registrar(
                        clienteId,
                        locadoraId,
                        gerenteId,
                        new RegistrarReconsideracaoDTO(
                                DecisaoReconsideracao.APROVADA,
                                "Reconsideracao apos reuniao."
                        )
                );

        assertEquals(
                StatusAdimplencia.ADIMPLENTE,
                resultado.statusAdimplencia()
        );

        assertEquals("ADIMPLENTE", statusCliente());

        assertEquals(2, cicloAtual());

        assertEquals(0L, ocorrenciasAtuais());

        assertEquals(2L, totalOcorrencias());

        assertEquals(1L, totalReconsideracoes());

        ContratoTeste terceiro =
                criarContrato(1, 2);

        ContratoTeste quarto =
                criarContrato(1, 3);

        devolver(terceiro.itens().getFirst());

        assertEquals(1L, ocorrenciasAtuais());

        assertEquals("ADIMPLENTE", statusCliente());

        devolver(quarto.itens().getFirst());

        assertEquals(2L, ocorrenciasAtuais());

        assertEquals("INADIMPLENTE", statusCliente());

        assertEquals(4L, totalOcorrencias());
    }

    @Test
    void devePreservarCicloQuandoReconsideracaoRejeitada() {

        tornarClienteInadimplente();

        reconsideracaoService.registrar(
                clienteId,
                locadoraId,
                gerenteId,
                new RegistrarReconsideracaoDTO(
                        DecisaoReconsideracao.REJEITADA,
                        "Reconsideracao nao autorizada."
                )
        );

        assertEquals("INADIMPLENTE", statusCliente());

        assertEquals(1, cicloAtual());

        assertEquals(2L, ocorrenciasAtuais());

        assertEquals(2L, totalOcorrencias());

        assertEquals(1L, totalReconsideracoes());

        assertEquals(
                "REJEITADA",
                jdbc.queryForObject(
                        """
                        SELECT decisao
                        FROM reconsideracao_cliente
                        WHERE cliente_id = ?
                        """,
                        String.class,
                        clienteId
                )
        );
    }

    private void tornarClienteInadimplente() {

        ContratoTeste primeiro =
                criarContrato(1, 2);

        ContratoTeste segundo =
                criarContrato(1, 3);

        devolver(primeiro.itens().getFirst());

        devolver(segundo.itens().getFirst());

        assertEquals("INADIMPLENTE", statusCliente());
    }

    private DevolucaoDto devolver(Long itemId) {

        return devolucaoService.registrar(
                new RegistrarDevolucaoDto(
                        itemId,
                        "Devolucao de teste"
                ),
                locadoraId,
                atendenteId
        );
    }

    private String statusCliente() {

        return jdbc.queryForObject(
                """
                SELECT status_adimplencia
                FROM cliente
                WHERE id = ?
                """,
                String.class,
                clienteId
        );
    }

    private Integer cicloAtual() {

        return jdbc.queryForObject(
                """
                SELECT ciclo_adimplencia
                FROM cliente
                WHERE id = ?
                """,
                Integer.class,
                clienteId
        );
    }

    private Long ocorrenciasAtuais() {

        return jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM ocorrencia_atraso
                WHERE cliente_id = ?
                  AND locadora_id = ?
                  AND ciclo = ?
                """,
                Long.class,
                clienteId,
                locadoraId,
                cicloAtual()
        );
    }

    private Long totalOcorrencias() {

        return jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM ocorrencia_atraso
                WHERE cliente_id = ?
                  AND locadora_id = ?
                """,
                Long.class,
                clienteId,
                locadoraId
        );
    }

    private Long totalReconsideracoes() {

        return jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM reconsideracao_cliente
                WHERE cliente_id = ?
                  AND locadora_id = ?
                """,
                Long.class,
                clienteId,
                locadoraId
        );
    }

    private Long quantidadeFaturas(Long contratoId) {

        return jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM fatura
                WHERE contrato_id = ?
                """,
                Long.class,
                contratoId
        );
    }

    private Long criarUsuario(String perfil) {

        // Gerentes ativos precisam possuir CPF.
        // CPF sintetico utilizado exclusivamente no teste.
        String cpf = "GERENTE".equals(perfil)
                ? String.format(
                        Locale.ROOT,
                        "%011d",
                        ThreadLocalRandom.current()
                                .nextLong(100_000_000_000L)
                )
                : null;

        return inserir("""
                INSERT INTO usuario (
                    locadora_id,
                    nome,
                    email,
                    cpf,
                    senha_hash,
                    perfil,
                    status,
                    data_ativacao
                )
                VALUES (
                    ?,
                    'Usuario Teste',
                    ?,
                    ?,
                    ?,
                    ?,
                    'ATIVO',
                    current_timestamp
                )
                RETURNING id
                """,
                locadoraId,
                UUID.randomUUID() + "@teste.example",
                cpf,
                "$2a$10$" + "a".repeat(53),
                perfil
        );
    }

    private ContratoTeste criarContrato(
            int quantidade,
            int diasAtraso) {

        LocalDate fim =
                hoje.minusDays(diasAtraso);

        LocalDate inicio =
                fim.minusDays(10);

        BigDecimal valor = BigDecimal.valueOf(
                quantidade * 1000L
        );

        Long solicitacaoId = inserir("""
                INSERT INTO solicitacao (
                    locadora_id,
                    cliente_id,
                    solicitante_id,
                    data_inicio_prevista,
                    data_fim_prevista,
                    valor_estimado
                )
                VALUES (?, ?, ?, ?, ?, ?)
                RETURNING id
                """,
                locadoraId,
                clienteId,
                atendenteId,
                inicio,
                fim,
                valor
        );

        Long itemSolicitacaoId = inserir("""
                INSERT INTO item_solicitacao (
                    locadora_id,
                    solicitacao_id,
                    modelo_id,
                    quantidade,
                    valor_unitario
                )
                VALUES (?, ?, ?, ?, 1000.00)
                RETURNING id
                """,
                locadoraId,
                solicitacaoId,
                modeloId,
                quantidade
        );

        jdbc.update("""
                UPDATE solicitacao
                SET status = 'APROVADA',
                    aprovador_id = ?,
                    data_analise = current_timestamp
                WHERE id = ?
                """,
                gerenteId,
                solicitacaoId
        );

        Long contratoId = inserir("""
                INSERT INTO contrato (
                    locadora_id,
                    solicitacao_id,
                    cliente_id,
                    responsavel_id,
                    data_inicio_prevista,
                    data_fim_prevista,
                    valor_locacao,
                    valor_entrega,
                    percentual_multa_atraso,
                    condicoes_devolucao
                )
                VALUES (
                    ?, ?, ?, ?, ?, ?, ?,
                    0, 10,
                    'Devolucao conforme prazo contratado'
                )
                RETURNING id
                """,
                locadoraId,
                solicitacaoId,
                clienteId,
                gerenteId,
                inicio,
                fim,
                valor
        );

        List<Long> itens = new ArrayList<>();

        for (int i = 0; i < quantidade; i++) {

            Long unidadeId = inserir("""
                    INSERT INTO unidade_equipamento (
                        locadora_id,
                        modelo_id,
                        numero_serie
                    )
                    VALUES (?, ?, ?)
                    RETURNING id
                    """,
                    locadoraId,
                    modeloId,
                    UUID.randomUUID().toString()
            );

            Long itemId = inserir("""
                    INSERT INTO item_contrato (
                        locadora_id,
                        contrato_id,
                        solicitacao_id,
                        item_solicitacao_id,
                        modelo_id,
                        unidade_id,
                        valor_diaria,
                        data_inicio_prevista,
                        data_fim_prevista,
                        status_contrato
                    )
                    VALUES (
                        ?, ?, ?, ?, ?, ?,
                        100.00, ?, ?, 'PLANEJADO'
                    )
                    RETURNING id
                    """,
                    locadoraId,
                    contratoId,
                    solicitacaoId,
                    itemSolicitacaoId,
                    modeloId,
                    unidadeId,
                    inicio,
                    fim
            );

            itens.add(itemId);
        }

        jdbc.update("""
                UPDATE contrato
                SET status = 'ATIVO',
                    data_retirada = ?,
                    responsavel_retirada_id = ?
                WHERE id = ?
                """,
                java.sql.Timestamp.from(
                        inicio.atStartOfDay(FUSO)
                                .toInstant()
                ),
                atendenteId,
                contratoId
        );

        return new ContratoTeste(
                contratoId,
                itens
        );
    }

    private Long inserir(
            String sql,
            Object... argumentos) {

        return jdbc.queryForObject(
                sql,
                Long.class,
                argumentos
        );
    }

    private String cnpj() {

        return UUID.randomUUID()
                .toString()
                .replaceAll("[^0-9]", "")
                .concat("00000000000000")
                .substring(0, 14);
    }
}