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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class DisponibilidadeRepositoryTest {
    private static final LocalDate HOJE = LocalDate.now();
    @Autowired
    private DisponibilidadeRepository repository;
    @Autowired
    private JdbcTemplate jdbc;
    private long andaime;
    private long betoneira;
    private long cliente;

    @BeforeEach
    void preparar() {
        cliente = jdbc.queryForObject("INSERT INTO cliente (razao_social, cnpj, contato_nome, email, telefone, cep, logradouro, numero, bairro, cidade, uf) VALUES ('Construtora Teste', '99888777000166', 'Ana', 'ana@teste.com', '79999999999', '49000000', 'Rua A', '1', 'Centro', 'Aracaju', 'SE') RETURNING id", Long.class);
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
    void vistoriaSoBloqueiaNasProximas48Horas() {
        long emVistoria = unidade(andaime, "AND-1", "EM_VISTORIA", true);
        assertThat(ids(repository.buscarUnidadesLivres(List.of(andaime), periodo(1, 5)))).isEmpty();
        assertThat(ids(repository.buscarUnidadesLivres(List.of(andaime), periodo(3, 5)))).containsExactly(emVistoria);
    }

    @Test
    void unidadeComDevolucaoAtrasadaFicaIndisponivel() {
        long atrasada = unidade(andaime, "AND-1", "LOCADA", true);
        long devolvida = unidade(andaime, "AND-2", "DISPONIVEL", true);
        reservar(atrasada, -10, -2, -10, null);
        reservar(devolvida, -10, -2, -10, -2);
        assertThat(ids(repository.buscarUnidadesLivres(List.of(andaime), periodo(10, 20)))).containsExactly(devolvida);
    }

    @Test
    void travaUnidadesSemErro() {
        unidade(andaime, "AND-1", "DISPONIVEL", true);
        repository.travarUnidadesDosModelos(List.of(andaime, betoneira));
    }

    private long modelo(String nome, String categoria, String diaria) {
        return jdbc.queryForObject("INSERT INTO modelo_equipamento (nome, categoria, valor_diaria_base) VALUES (?, ?, ?::numeric) RETURNING id", Long.class, nome, categoria, diaria);
    }

    private long unidade(long modeloId, String serie, String status, boolean ativo) {
        return jdbc.queryForObject("INSERT INTO unidade_equipamento (modelo_id, numero_serie, status, ativo) VALUES (?, ?, ?, ?) RETURNING id", Long.class, modeloId, serie, status, ativo);
    }

    private void reservar(long unidadeId, int inicio, int fim, Integer retirada, Integer devolucao) {
        long contrato = jdbc.queryForObject("INSERT INTO contrato (cliente_id, data_inicio_prevista, data_fim_prevista) VALUES (?, ?, ?) RETURNING id", Long.class, cliente, HOJE.plusDays(inicio), HOJE.plusDays(fim));
        jdbc.update("INSERT INTO item_contrato (contrato_id, unidade_id, data_inicio_prevista, data_fim_prevista, data_inicio_real, data_devolucao_real, valor_diaria_contratada) VALUES (?, ?, ?, ?, ?, ?, 50)", contrato, unidadeId, HOJE.plusDays(inicio), HOJE.plusDays(fim), retirada == null ? null : HOJE.plusDays(retirada), devolucao == null ? null : HOJE.plusDays(devolucao));
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
}
