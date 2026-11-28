package br.ufs.ziploc.acesso.service;

import br.ufs.ziploc.acesso.domain.StatusUsuario;
import br.ufs.ziploc.acesso.dto.UsuarioResponseDTO;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(properties = {
        "ziploc.lgpd.cron=-",
        "ziploc.lgpd.usuarios-inativos-dias=365"
})
class AnonimizacaoUsuariosIntegracaoTest {

    private static final Instant AGORA =
            Instant.now().truncatedTo(ChronoUnit.SECONDS);

    private static final Instant LIMITE =
            AGORA.minus(365, ChronoUnit.DAYS);

    @Autowired
    private AnonimizacaoUsuariosService service;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private EntityManager entityManager;

    private long locadora;
    private String sufixo;

    @BeforeEach
    void preparar() {
        sufixo = UUID.randomUUID().toString();

        locadora = jdbc.queryForObject(
                """
                INSERT INTO locadora (
                    razao_social, cnpj, cep_sede,
                    logradouro_sede, numero_sede, bairro_sede,
                    cidade_sede, uf_sede, raio_entrega_km,
                    percentual_multa_atraso
                )
                VALUES (
                    'Locadora Teste LGPD', ?, '49000000',
                    'Rua A', '1', 'Centro',
                    'Aracaju', 'SE', 100, 10
                )
                RETURNING id
                """,
                Long.class, cnpj());
    }

    @AfterEach
    void limpar() {
        if (locadora != 0) {
            jdbc.update(
                    "DELETE FROM usuario WHERE locadora_id = ?",
                    locadora);

            jdbc.update(
                    "DELETE FROM locadora WHERE id = ?",
                    locadora);
        }
    }

    @Test
    void anonimizaSomenteElegiveisEPreservaHistorico() {
        TransactionTemplate tx =
                new TransactionTemplate(transactionManager);

        tx.executeWithoutResult(transacao -> {
            try {
                long antigo = criarUsuario(
                        "antigo", "INATIVO",
                        LIMITE.minus(30, ChronoUnit.DAYS));

                long limiteExato = criarUsuario(
                        "limite", "INATIVO", LIMITE);

                long recente = criarUsuario(
                        "recente", "INATIVO",
                        LIMITE.plusSeconds(1));

                long ativo = criarUsuario(
                        "ativo", "ATIVO", null);

                long pendente = criarUsuario(
                        "pendente", "PENDENTE", null);

                Map<String, Object> original = consultar(antigo);

                int total = service.anonimizarInativos(AGORA);

                entityManager.flush();

                assertThat(total).isGreaterThanOrEqualTo(2);

                verificarAnonimizado(antigo);
                verificarAnonimizado(limiteExato);

                assertThat(consultar(recente).get("anonimizado_em"))
                        .isNull();

                assertThat(consultar(ativo).get("anonimizado_em"))
                        .isNull();

                assertThat(consultar(pendente).get("anonimizado_em"))
                        .isNull();

                assertThat(consultar(recente).get("email"))
                        .isEqualTo(email("recente"));

                assertThat(consultar(ativo).get("status"))
                        .isEqualTo("ATIVO");

                assertThat(consultar(pendente).get("status"))
                        .isEqualTo("PENDENTE");

                Map<String, Object> anonimizado = consultar(antigo);

                assertThat(anonimizado.get("id"))
                        .isEqualTo(original.get("id"));

                assertThat(anonimizado.get("locadora_id"))
                        .isEqualTo(original.get("locadora_id"));

                assertThat(anonimizado.get("perfil"))
                        .isEqualTo(original.get("perfil"));

                assertThat(anonimizado.get("data_cadastro"))
                        .isEqualTo(original.get("data_cadastro"));

                assertThat(anonimizado.get("data_ativacao"))
                        .isEqualTo(original.get("data_ativacao"));

                assertThat(anonimizado.get("data_inativacao"))
                        .isEqualTo(original.get("data_inativacao"));

                assertThat(service.anonimizarInativos(AGORA))
                        .isZero();

                entityManager.flush();

                assertThat(consultar(antigo))
                        .isEqualTo(anonimizado);

                assertThat(usuarioService.consultarPerfilAtivo(antigo))
                        .isEmpty();

                assertThrows(
                        OperacaoUsuarioInvalidaException.class,
                        () -> usuarioService.reativar(antigo));

            } finally {
                transacao.setRollbackOnly();
            }
        });
    }

    @Test
    void reativacaoConcorrenteComAnonimizacaoMantemEstadoConsistente()
            throws Exception {

        // Impede a execucao destrutiva em outros bancos.
        assertThat(jdbc.queryForObject(
                "SELECT current_database()", String.class))
                .isEqualTo("ziploc_lgpd_concorrencia");

        // Garante que nao existem outros candidatos a anonimizacao.
        assertThat(jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM usuario
                WHERE status = 'INATIVO'
                  AND anonimizado_em IS NULL
                  AND data_inativacao <= ?
                """,
                Integer.class, dataSql(LIMITE)))
                .isZero();

        long usuario = criarUsuario(
                "concorrente", "INATIVO",
                LIMITE.minus(30, ChronoUnit.DAYS));

        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Integer> anonimizacao = executor.submit(() -> {
                largada.await();
                return service.anonimizarInativos(AGORA);
            });

            Future<Object> reativacao = executor.submit(() -> {
                largada.await();

                try {
                    return usuarioService.reativar(usuario);
                } catch (OperacaoUsuarioInvalidaException e) {
                    return e;
                }
            });

            largada.countDown();

            int total = anonimizacao.get(30, TimeUnit.SECONDS);
            Object resultado = reativacao.get(30, TimeUnit.SECONDS);

            Map<String, Object> estado = consultar(usuario);

            if (resultado instanceof UsuarioResponseDTO dto) {

                assertThat(total).isZero();

                assertThat(dto.status())
                        .isEqualTo(StatusUsuario.ATIVO);

                assertThat(estado.get("status"))
                        .isEqualTo("ATIVO");

                assertThat(estado.get("anonimizado_em"))
                        .isNull();

                assertThat(estado.get("data_inativacao"))
                        .isNull();

                assertThat(estado.get("senha_hash"))
                        .isNotNull();

                assertThat(estado.get("email"))
                        .isEqualTo(email("concorrente"));

            } else {

                assertThat(resultado)
                        .isInstanceOf(OperacaoUsuarioInvalidaException.class);

                assertThat(((OperacaoUsuarioInvalidaException) resultado)
                        .getMessage())
                        .isEqualTo("Um usuário anonimizado não pode ser reativado");

                assertThat(total).isEqualTo(1);

                verificarAnonimizado(usuario);

                assertThat(usuarioService.consultarPerfilAtivo(usuario))
                        .isEmpty();

                assertThrows(
                        OperacaoUsuarioInvalidaException.class,
                        () -> usuarioService.reativar(usuario));
            }

            assertThat(service.anonimizarInativos(AGORA))
                    .isZero();

            assertThat(consultar(usuario))
                    .isEqualTo(estado);

        } finally {
            executor.shutdownNow();
            executor.awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    @Test
    void bancoRejeitaInativoSemDataDeInativacao() {
        long usuario = criarUsuario("sem-data", "ATIVO", null);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> jdbc.update(
                        "UPDATE usuario SET status = 'INATIVO' WHERE id = ?",
                        usuario));
    }

    @Test
    void bancoRejeitaAtivoComDataDeInativacao() {
        long usuario = criarUsuario("ativo-data", "ATIVO", null);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> jdbc.update(
                        "UPDATE usuario SET data_inativacao = NOW() WHERE id = ?",
                        usuario));
    }

    @Test
    void bancoRejeitaEmailDoDominioReservado() {
        long usuario = criarUsuario(
                "email-reservado", "ATIVO", null);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> jdbc.update(
                        "UPDATE usuario SET email = ? WHERE id = ?",
                        "anonimizado-" + usuario + "@ANONIMIZADO.INVALID",
                        usuario));
    }

    @Test
    void bancoRejeitaEmailAnonimizadoDeOutroId() {
        long usuario = criarUsuario(
                "email-incorreto", "INATIVO", LIMITE);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> jdbc.update(
                        """
                        UPDATE usuario
                        SET anonimizado_em = NOW(),
                            cpf = NULL,
                            senha_hash = NULL,
                            cargo = NULL,
                            nome = 'ANONIMIZADO',
                            email = ?
                        WHERE id = ?
                        """,
                        "anonimizado-0@anonimizado.invalid",
                        usuario));
    }

    private long criarUsuario(
            String identificador,
            String status,
            Instant dataInativacao) {

        boolean pendente = "PENDENTE".equals(status);

        return jdbc.queryForObject(
                """
                INSERT INTO usuario (
                    locadora_id, nome, email, cpf,
                    senha_hash, perfil, cargo, status,
                    data_cadastro, data_ativacao, data_inativacao
                )
                VALUES (
                    ?, ?, ?, ?, ?, 'ATENDENTE', 'Teste LGPD', ?,
                    ?, ?, ?
                )
                RETURNING id
                """,
                Long.class,
                locadora,
                "Usuario " + identificador,
                email(identificador),
                cpf(),
                pendente ? null : "$2a$10$" + "A".repeat(53),
                status,
                dataSql(AGORA.minus(800, ChronoUnit.DAYS)),
                pendente
                        ? null
                        : dataSql(AGORA.minus(799, ChronoUnit.DAYS)),
                dataSql(dataInativacao));
    }

    private void verificarAnonimizado(long id) {
        Map<String, Object> usuario = consultar(id);

        assertThat(usuario.get("nome"))
                .isEqualTo("ANONIMIZADO");

        assertThat(usuario.get("email"))
                .isEqualTo("anonimizado-" + id + "@anonimizado.invalid");

        assertThat(usuario.get("status"))
                .isEqualTo("INATIVO");

        assertThat(usuario.get("cpf")).isNull();
        assertThat(usuario.get("senha_hash")).isNull();
        assertThat(usuario.get("cargo")).isNull();
        assertThat(usuario.get("anonimizado_em")).isNotNull();
        assertThat(usuario.get("data_inativacao")).isNotNull();
    }

    private Map<String, Object> consultar(long id) {
        return jdbc.queryForMap(
                "SELECT * FROM usuario WHERE id = ?", id);
    }

    private String email(String identificador) {
        return "lgpd." + identificador + "." + sufixo + "@teste.com";
    }

    private static OffsetDateTime dataSql(Instant instante) {
        return instante == null
                ? null
                : instante.atOffset(ZoneOffset.UTC);
    }

    private static String cnpj() {
        return String.format(
                "%014d",
                Math.floorMod(
                        UUID.randomUUID().getMostSignificantBits(),
                        100_000_000_000_000L));
    }

    private static String cpf() {
        return String.format(
                "%011d",
                Math.floorMod(
                        UUID.randomUUID().getMostSignificantBits(),
                        100_000_000_000L));
    }
}