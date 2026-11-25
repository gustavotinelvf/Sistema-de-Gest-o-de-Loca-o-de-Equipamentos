package br.ufs.ziploc.locacao.service;

import br.ufs.ziploc.disponibilidade.api.IndisponibilidadeException;
import br.ufs.ziploc.locacao.dto.ContratoDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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
    @Autowired
    private SolicitacaoService service;
    @Autowired
    private JdbcTemplate jdbc;
    private long cliente;
    private long gerente;
    private long modelo;
    private long unidade;

    @BeforeEach
    void preparar() {
        cliente = jdbc.queryForObject("INSERT INTO cliente (razao_social, cnpj, contato_nome, email, telefone, cep, logradouro, numero, bairro, cidade, uf) VALUES ('Construtora Concorrente', '99888777000247', 'Ana', 'concorrencia@teste.com', '79999999999', '49000000', 'Rua A', '1', 'Centro', 'Aracaju', 'SE') RETURNING id", Long.class);
        gerente = jdbc.queryForObject("INSERT INTO usuario (nome, email, senha_hash, perfil, cpf) VALUES ('Gerente Teste', 'gerente.concorrencia@teste.com', 'x', 'GERENTE', '99988877766') RETURNING id", Long.class);
        modelo = jdbc.queryForObject("INSERT INTO modelo_equipamento (nome, categoria, valor_diaria_base) VALUES ('Andaime concorrente', 'Andaime', 50) RETURNING id", Long.class);
        unidade = jdbc.queryForObject("INSERT INTO unidade_equipamento (modelo_id, numero_serie) VALUES (?, 'CONC-1') RETURNING id", Long.class, modelo);
    }

    @AfterEach
    void limpar() {
        jdbc.update("DELETE FROM item_contrato WHERE unidade_id = ?", unidade);
        jdbc.update("DELETE FROM contrato WHERE cliente_id = ?", cliente);
        jdbc.update("DELETE FROM item_solicitacao WHERE modelo_id = ?", modelo);
        jdbc.update("DELETE FROM solicitacao WHERE cliente_id = ?", cliente);
        jdbc.update("DELETE FROM unidade_equipamento WHERE id = ?", unidade);
        jdbc.update("DELETE FROM modelo_equipamento WHERE id = ?", modelo);
        jdbc.update("DELETE FROM usuario WHERE id = ?", gerente);
        jdbc.update("DELETE FROM cliente WHERE id = ?", cliente);
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
        assertThat(jdbc.queryForObject("SELECT percentual_multa FROM contrato WHERE id = ?", BigDecimal.class, contrato.id())).isEqualByComparingTo("10");
        assertThat(jdbc.queryForObject("SELECT unidade_id FROM item_contrato WHERE contrato_id = ?", Long.class, contrato.id())).isEqualTo(unidade);
    }

    @Test
    void mesmaSolicitacaoSoEAprovadaUmaVez() throws Exception {
        long id = solicitacao();
        List<Object> resultados = aprovarJuntas(id, id);
        assertThat(resultados).filteredOn(ContratoDTO.class::isInstance).hasSize(1);
        assertThat(resultados).filteredOn(IllegalStateException.class::isInstance).hasSize(1);
    }

    private List<Object> aprovarJuntas(long... ids) throws Exception {
        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(ids.length);
        try {
            List<Future<Object>> tarefas = new ArrayList<>();
            for (long id : ids) tarefas.add(executor.submit(() -> {
                largada.await();
                try {
                    return service.aprovar(id, gerente, BigDecimal.TEN);
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
        long id = jdbc.queryForObject("INSERT INTO solicitacao (cliente_id, registrado_por_id, data_inicio_desejada, data_fim_desejada) VALUES (?, ?, ?, ?) RETURNING id", Long.class, cliente, gerente, INICIO, FIM);
        jdbc.update("INSERT INTO item_solicitacao (solicitacao_id, modelo_id, quantidade) VALUES (?, ?, 1)", id, modelo);
        return id;
    }

    private int contar(String status) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM solicitacao WHERE cliente_id = ? AND status = ?", Integer.class, cliente, status);
    }
}
