package br.ufs.ziploc.manutencao;

import br.ufs.ziploc.manutencao.domain.TipoManutencao;
import br.ufs.ziploc.manutencao.dto.ManutencaoDto;
import br.ufs.ziploc.manutencao.service.ManutencaoService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ManutencaoIntegracaoTest {

    @Autowired
    private ManutencaoService service;

    @Autowired
    private JdbcTemplate jdbc;

    private long locadora;
    private long tecnico;
    private long unidade;
    private String numeroSerie;

    @BeforeEach
    void preparar() {
        String sufixo = UUID.randomUUID().toString();
        numeroSerie = "MAN-" + sufixo;

        locadora = jdbc.queryForObject(
                "INSERT INTO locadora (razao_social, cnpj, cep_sede, logradouro_sede, numero_sede, bairro_sede, cidade_sede, uf_sede, raio_entrega_km, percentual_multa_atraso) VALUES ('Locadora Manutencao', ?, '49000000', 'Rua A', '1', 'Centro', 'Aracaju', 'SE', 100, 10) RETURNING id",
                Long.class, cnpj());

        tecnico = jdbc.queryForObject(
                "INSERT INTO usuario (locadora_id, nome, email, senha_hash, perfil, status, data_ativacao) VALUES (?, 'Tecnico Teste', ?, ?, 'TECNICO', 'ATIVO', now()) RETURNING id",
                Long.class, locadora,
                "tecnico." + sufixo + "@teste.com",
                "$2a$10$" + "A".repeat(53));

        long modelo = jdbc.queryForObject(
                "INSERT INTO modelo_equipamento (locadora_id, nome, categoria, especificacoes, valor_diaria) VALUES (?, 'Andaime', 'Andaime', 'Equipamento de teste', 50) RETURNING id",
                Long.class, locadora);

        unidade = jdbc.queryForObject(
                "INSERT INTO unidade_equipamento (locadora_id, modelo_id, numero_serie) VALUES (?, ?, ?) RETURNING id",
                Long.class, locadora, modelo, numeroSerie);
    }

    @Test
    void deveIniciarManutencaoEAlterarStatusPeloTrigger() {
        assertThat(statusUnidade()).isEqualTo("DISPONIVEL");

        ManutencaoDto resultado = service.iniciarAvulsa(
                locadora, numeroSerie, TipoManutencao.PREVENTIVA,
                "Revisão preventiva", null, tecnico);

        assertThat(resultado.id()).isNotNull();
        assertThat(resultado.unidadeId()).isEqualTo(unidade);
        assertThat(resultado.tecnicoId()).isEqualTo(tecnico);
        assertThat(resultado.tipo()).isEqualTo(TipoManutencao.PREVENTIVA);
        assertThat(resultado.dataConclusao()).isNull();

        assertThat(statusUnidade()).isEqualTo("EM_MANUTENCAO");

        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM manutencao WHERE unidade_id = ? AND data_conclusao IS NULL",
                Long.class, unidade))
                .isEqualTo(1L);
    }

    @Test
    void deveConcluirManutencaoELiberarUnidadePeloTrigger() {
        ManutencaoDto aberta = service.iniciarAvulsa(
                locadora, numeroSerie, TipoManutencao.CORRETIVA,
                "Substituição de peça", null, tecnico);

        assertThat(statusUnidade()).isEqualTo("EM_MANUTENCAO");

        ManutencaoDto concluida = service.concluir(
                aberta.id(), new BigDecimal("150.55"), null);

        assertThat(concluida.dataConclusao()).isNotNull();

        assertThat(concluida.custoGerado())
                .isEqualByComparingTo("150.55");

        assertThat(statusUnidade()).isEqualTo("DISPONIVEL");

        assertThat(jdbc.queryForObject(
                "SELECT custo_gerado FROM manutencao WHERE id = ?",
                BigDecimal.class, aberta.id()))
                .isEqualByComparingTo("150.55");

        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM manutencao WHERE unidade_id = ? AND data_conclusao IS NULL",
                Long.class, unidade))
                .isZero();
    }

    @Test
    void deveConsultarHistoricoDaUnidade() {
        ManutencaoDto primeira = service.iniciarAvulsa(
                locadora, numeroSerie, TipoManutencao.PREVENTIVA,
                "Primeira revisão", null, tecnico);

        service.concluir(primeira.id(), BigDecimal.ZERO, null);

        ManutencaoDto segunda = service.iniciarAvulsa(
                locadora, numeroSerie, TipoManutencao.CORRETIVA,
                "Segunda revisão", null, tecnico);

        var historico = service.historico(numeroSerie);

        assertThat(historico).hasSize(2);
        assertThat(historico.get(0).id()).isEqualTo(segunda.id());
        assertThat(historico.get(1).id()).isEqualTo(primeira.id());
    }

    private String statusUnidade() {
        return jdbc.queryForObject(
                "SELECT status FROM unidade_equipamento WHERE id = ?",
                String.class, unidade);
    }

    private static String cnpj() {
        return String.format("%014d",
                Math.floorMod(UUID.randomUUID().getMostSignificantBits(),
                        100_000_000_000_000L));
    }
}