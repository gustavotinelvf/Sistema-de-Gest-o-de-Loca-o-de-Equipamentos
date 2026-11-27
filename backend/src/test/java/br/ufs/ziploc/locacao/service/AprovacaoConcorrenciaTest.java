package br.ufs.ziploc.locacao.service;

import br.ufs.ziploc.disponibilidade.api.IndisponibilidadeException;
import br.ufs.ziploc.locacao.dto.ContratoDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AprovacaoConcorrenciaTest {
    private static final LocalDate INICIO = LocalDate.now().plusDays(10);
    private static final LocalDate FIM = INICIO.plusDays(9);
    private static final String CONDICOES = "Devolver os equipamentos no prazo contratado.";
    @Autowired
    private SolicitacaoService service;
    @Autowired
    private JdbcTemplate jdbc;
    private long locadora;
    private long cliente;
    private long gerente;
    private long modelo;
    private long unidade;

    @BeforeEach
    void preparar() {
        String sufixo = UUID.randomUUID().toString();
        locadora = jdbc.queryForObject("INSERT INTO locadora (razao_social, cnpj, cep_sede, logradouro_sede, numero_sede, bairro_sede, cidade_sede, uf_sede, raio_entrega_km, percentual_multa_atraso) VALUES ('Locadora Concorrencia', ?, '49000000', 'Rua A', '1', 'Centro', 'Aracaju', 'SE', 100, 10) RETURNING id", Long.class, cnpj());
        cliente = jdbc.queryForObject("INSERT INTO cliente (locadora_id, razao_social, cnpj, representante_nome, email, telefone, cep, logradouro, numero, bairro, cidade, uf) VALUES (?, 'Construtora Concorrente', ?, 'Ana', ?, '79999999999', '49000000', 'Rua A', '1', 'Centro', 'Aracaju', 'SE') RETURNING id", Long.class, locadora, cnpj(), "cliente." + sufixo + "@teste.com");
        gerente = jdbc.queryForObject("INSERT INTO usuario (locadora_id, nome, email, cpf, senha_hash, perfil, status, data_ativacao) VALUES (?, 'Gerente Teste', ?, ?, ?, 'GERENTE', 'ATIVO', now()) RETURNING id", Long.class, locadora, "gerente." + sufixo + "@teste.com", cpf(), "$2a$10$" + "A".repeat(53));
        modelo = jdbc.queryForObject("INSERT INTO modelo_equipamento (locadora_id, nome, categoria, especificacoes, valor_diaria) VALUES (?, 'Andaime concorrente', 'Andaime', 'Equipamento de teste', 50) RETURNING id", Long.class, locadora);
        unidade = jdbc.queryForObject("INSERT INTO unidade_equipamento (locadora_id, modelo_id, numero_serie) VALUES (?, ?, 'CONC-1') RETURNING id", Long.class, locadora, modelo);
    }

    @Test
    void duasSolicitacoesDisputamAUltimaUnidade() throws Exception {
        List<Object> resultados = aprovarJuntas(solicitacao(), solicitacao());
        assertThat(resultados).filteredOn(ContratoDTO.class::isInstance).hasSize(1);
        assertThat(resultados).filteredOn(IndisponibilidadeException.class::isInstance).hasSize(1);
        assertThat(contar("APROVADA")).isEqualTo(1);
        assertThat(contar("PENDENTE")).isEqualTo(1);
        ContratoDTO contrato = (ContratoDTO) resultados.stream().filter(ContratoDTO.class::isInstance).findFirst().orElseThrow();
        assertThat(contrato.valorTotal()).isEqualByComparingTo("500.00");
        assertThat(jdbc.queryForObject("SELECT percentual_multa_atraso FROM contrato WHERE id = ?", BigDecimal.class, contrato.id())).isEqualByComparingTo("10");
        assertThat(jdbc.queryForObject("SELECT unidade_id FROM item_contrato WHERE contrato_id = ?", Long.class, contrato.id())).isEqualTo(unidade);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM item_contrato WHERE unidade_id = ?", Integer.class, unidade)).isEqualTo(1);
    }

    @Test
    void mesmaSolicitacaoSoEAprovadaUmaVez() throws Exception {
        long id = solicitacao();
        List<Object> resultados = aprovarJuntas(id, id);
        assertThat(resultados).filteredOn(ContratoDTO.class::isInstance).hasSize(1);
        assertThat(resultados).filteredOn(IllegalStateException.class::isInstance).hasSize(1);
        assertThat(contar("APROVADA")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM contrato WHERE solicitacao_id = ?", Integer.class, id)).isEqualTo(1);
    }

    private List<Object> aprovarJuntas(long... ids) throws Exception {
        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(ids.length);
        try {
            List<Future<Object>> tarefas = new ArrayList<>();
            for (long id : ids) tarefas.add(executor.submit(() -> {
                largada.await();
                try {
                    return service.aprovar(id, gerente, BigDecimal.TEN, CONDICOES);
                } catch (RuntimeException e) {
                    return e;
                }
            }));
            largada.countDown();
            List<Object> resultados = new ArrayList<>();
            for (Future<Object> tarefa : tarefas) resultados.add(tarefa.get(30, TimeUnit.SECONDS));
            return resultados;
        } finally {
            executor.shutdownNow();
        }
    }

    private long solicitacao() {
        long id = jdbc.queryForObject("INSERT INTO solicitacao (locadora_id, cliente_id, solicitante_id, data_inicio_prevista, data_fim_prevista, valor_estimado) VALUES (?, ?, ?, ?, ?, 500) RETURNING id", Long.class, locadora, cliente, gerente, INICIO, FIM);
        jdbc.update("INSERT INTO item_solicitacao (locadora_id, solicitacao_id, modelo_id, quantidade, valor_unitario) VALUES (?, ?, ?, 1, 50)", locadora, id, modelo);
        return id;
    }

    private int contar(String status) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM solicitacao WHERE cliente_id = ? AND status = ?", Integer.class, cliente, status);
    }

    private static String cnpj() {
        return String.format("%014d", Math.floorMod(UUID.randomUUID().getMostSignificantBits(), 100_000_000_000_000L));
    }

    private static String cpf() {
        return String.format("%011d", Math.floorMod(UUID.randomUUID().getMostSignificantBits(), 100_000_000_000L));
    }
}