package br.ufs.ziploc.disponibilidade.repository;

import br.ufs.ziploc.disponibilidade.api.DisponibilidadeDTO;
import br.ufs.ziploc.disponibilidade.api.UnidadeDTO;
import br.ufs.ziploc.disponibilidade.domain.Periodo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class DisponibilidadeRepositoryTest {
    private static final LocalDate HOJE = LocalDate.now();
    @Autowired
    private DisponibilidadeRepository repository;
    @Autowired
    private JdbcTemplate jdbc;
    private long locadora;
    private long gerente;
    private long andaime;
    private long betoneira;
    private long cliente;

    @BeforeEach
    void preparar() {
        locadora = jdbc.queryForObject("INSERT INTO locadora (razao_social, cnpj, cep_sede, logradouro_sede, numero_sede, bairro_sede, cidade_sede, uf_sede, raio_entrega_km, percentual_multa_atraso) VALUES ('Locadora Teste', '11222333000181', '49000000', 'Rua A', '1', 'Centro', 'Aracaju', 'SE', 100, 10) RETURNING id", Long.class);
        gerente = jdbc.queryForObject("INSERT INTO usuario (locadora_id, nome, email, cpf, senha_hash, perfil, status, data_ativacao) VALUES (?, 'Gerente Teste', 'gerente.disponibilidade@teste.com', ?, ?, 'GERENTE', 'ATIVO', now()) RETURNING id", Long.class, locadora, cpf(), "$2a$10$" + "A".repeat(53));
        cliente = jdbc.queryForObject("INSERT INTO cliente (locadora_id, razao_social, cnpj, representante_nome, email, telefone, cep, logradouro, numero, bairro, cidade, uf) VALUES (?, 'Construtora Teste', '99888777000166', 'Ana', 'ana@teste.com', '79999999999', '49000000', 'Rua A', '1', 'Centro', 'Aracaju', 'SE') RETURNING id", Long.class, locadora);
        andaime = modelo("Andaime tubular", "Andaime", "50.00");
        betoneira = modelo("Betoneira 400L", "Betoneira", "80.00");
    }

    @Test
    void contaApenasUnidadesAtivasForaDeManutencao() {
        unidade(andaime, "AND-1", "DISPONIVEL", true);
        unidade(andaime, "AND-2", "DISPONIVEL", true);
        unidade(andaime, "AND-3", "EM_MANUTENCAO", true);
        unidade(andaime, "AND-4", "DISPONIVEL", false);
        unidade(betoneira, "BET-1", "DISPONIVEL", true);
        assertThat(quantidade(repository.contarLivresPorModelo(periodo(10, 20), null), andaime)).isEqualTo(2);
        assertThat(repository.contarLivresPorModelo(periodo(10, 20), "Betoneira")).extracting(DisponibilidadeDTO::modeloId).containsExactly(betoneira);
    }

    @Test
    void excluiReservasSobrepostasComDatasInclusivas() {
        long livre = unidade(andaime, "AND-1", "DISPONIVEL", true);
        long reservada = unidade(andaime, "AND-2", "DISPONIVEL", true);
        long encostada = unidade(andaime, "AND-3", "DISPONIVEL", true);
        reservar(reservada, 15, 25, null, null);
        reservar(encostada, 20, 30, null, null);
        reservar(livre, 21, 30, null, null);
        assertThat(ids(repository.buscarUnidadesLivres(List.of(andaime), periodo(10, 20)))).containsExactly(livre);
    }

    @Test
    void vistoriaBloqueiaAteLiberacaoDaUnidade() {
        long emVistoria = unidade(andaime, "AND-1", "EM_VISTORIA", true);
        assertThat(ids(repository.buscarUnidadesLivres(List.of(andaime), periodo(1, 5)))).isEmpty();
        assertThat(ids(repository.buscarUnidadesLivres(List.of(andaime), periodo(3, 5)))).isEmpty();
        jdbc.update("UPDATE unidade_equipamento SET status = 'DISPONIVEL' WHERE id = ?", emVistoria);
        assertThat(ids(repository.buscarUnidadesLivres(List.of(andaime), periodo(3, 5)))).containsExactly(emVistoria);
    }

    @Test
    void unidadeComDevolucaoAtrasadaFicaIndisponivel() {
        long atrasada = unidade(andaime, "AND-1", "DISPONIVEL", true);
        long devolvida = unidade(andaime, "AND-2", "DISPONIVEL", true);
        reservar(atrasada, -10, -2, -10, null);
        reservar(devolvida, -10, -2, -10, -2);
        assertThat(jdbc.queryForObject("SELECT status FROM unidade_equipamento WHERE id = ?", String.class, atrasada)).isEqualTo("LOCADA");
        assertThat(jdbc.queryForObject("SELECT status FROM unidade_equipamento WHERE id = ?", String.class, devolvida)).isEqualTo("EM_VISTORIA");
        assertThat(ids(repository.buscarUnidadesLivres(List.of(andaime), periodo(10, 20)))).isEmpty();
        jdbc.update("UPDATE unidade_equipamento SET status = 'DISPONIVEL' WHERE id = ?", devolvida);
        assertThat(ids(repository.buscarUnidadesLivres(List.of(andaime), periodo(10, 20)))).containsExactly(devolvida);
    }

    @Test
    void travaUnidadesSemErro() {
        unidade(andaime, "AND-1", "DISPONIVEL", true);
        repository.travarUnidadesDosModelos(List.of(andaime, betoneira));
    }

    private long modelo(String nome, String categoria, String diaria) {
        return jdbc.queryForObject("INSERT INTO modelo_equipamento (locadora_id, nome, categoria, especificacoes, valor_diaria) VALUES (?, ?, ?, 'Equipamento de teste', ?::numeric) RETURNING id", Long.class, locadora, nome, categoria, diaria);
    }

    private long unidade(long modeloId, String serie, String status, boolean ativo) {
        return jdbc.queryForObject("INSERT INTO unidade_equipamento (locadora_id, modelo_id, numero_serie, status, ativo) VALUES (?, ?, ?, ?, ?) RETURNING id", Long.class, locadora, modeloId, serie, status, ativo);
    }

    private void reservar(long unidadeId, int inicio, int fim, Integer retirada, Integer devolucao) {
        long modeloId = jdbc.queryForObject("SELECT modelo_id FROM unidade_equipamento WHERE id = ?", Long.class, unidadeId);
        LocalDate dataInicio = HOJE.plusDays(inicio);
        LocalDate dataFim = HOJE.plusDays(fim);
        long solicitacao = jdbc.queryForObject("INSERT INTO solicitacao (locadora_id, cliente_id, solicitante_id, data_inicio_prevista, data_fim_prevista, valor_estimado) VALUES (?, ?, ?, ?, ?, ?) RETURNING id", Long.class, locadora, cliente, gerente, dataInicio, dataFim, (fim - inicio + 1) * 50);
        long itemSolicitacao = jdbc.queryForObject("INSERT INTO item_solicitacao (locadora_id, solicitacao_id, modelo_id, quantidade, valor_unitario) VALUES (?, ?, ?, 1, 50) RETURNING id", Long.class, locadora, solicitacao, modeloId);
        jdbc.update("UPDATE solicitacao SET status = 'APROVADA', aprovador_id = ?, data_analise = now() WHERE id = ?", gerente, solicitacao);
        long contrato = jdbc.queryForObject("INSERT INTO contrato (locadora_id, solicitacao_id, cliente_id, responsavel_id, data_inicio_prevista, data_fim_prevista, valor_locacao, percentual_multa_atraso, condicoes_devolucao) VALUES (?, ?, ?, ?, ?, ?, ?, 10, 'Devolver no prazo contratado') RETURNING id", Long.class, locadora, solicitacao, cliente, gerente, dataInicio, dataFim, (fim - inicio + 1) * 50);
        long itemContrato = jdbc.queryForObject("INSERT INTO item_contrato (locadora_id, contrato_id, solicitacao_id, item_solicitacao_id, modelo_id, unidade_id, data_inicio_prevista, data_fim_prevista, valor_diaria) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 50) RETURNING id", Long.class, locadora, contrato, solicitacao, itemSolicitacao, modeloId, unidadeId, dataInicio, dataFim);

        if (retirada != null) {
            jdbc.update("UPDATE contrato SET status = 'ATIVO', data_retirada = (?::date::timestamp AT TIME ZONE 'America/Sao_Paulo'), responsavel_retirada_id = ? WHERE id = ?", HOJE.plusDays(retirada), gerente, contrato);
        }

        if (devolucao != null) {
            jdbc.update("INSERT INTO devolucao (locadora_id, item_contrato_id, usuario_id, data_devolucao_real, multa_atraso) VALUES (?, ?, ?, (?::date::timestamp AT TIME ZONE 'America/Sao_Paulo'), 0)", locadora, itemContrato, gerente, HOJE.plusDays(devolucao));
        }
    }

    private static Periodo periodo(int inicio, int fim) {
        return new Periodo(HOJE.plusDays(inicio), HOJE.plusDays(fim));
    }

    private static List<Long> ids(List<UnidadeDTO> unidades) {
        return unidades.stream().map(UnidadeDTO::unidadeId).toList();
    }

    private static int quantidade(List<DisponibilidadeDTO> lista, long modeloId) {
        return lista.stream().filter(d -> d.modeloId() == modeloId).findFirst().map(DisponibilidadeDTO::quantidadeDisponivel).orElse(0);
    }

    private static String cpf() {
        return String.format("%011d",
                Math.floorMod(UUID.randomUUID().getMostSignificantBits(),
                        100_000_000_000L));
    }
}