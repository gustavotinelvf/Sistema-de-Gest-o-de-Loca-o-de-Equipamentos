package br.ufs.ziploc.cliente.repository;

import br.ufs.ziploc.cliente.domain.DecisaoReconsideracao;
import br.ufs.ziploc.cliente.domain.StatusAdimplencia;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ReconsideracaoRepository {

    private final JdbcTemplate jdbcTemplate;

    public record ClienteCiclo(
            Long id,
            Integer ciclo,
            StatusAdimplencia status,
            boolean ativo
    ) {
    }

    public Optional<ClienteCiclo> buscarClienteParaAtualizacao(
            Long clienteId,
            Long locadoraId) {

        String sql = """
                SELECT id,
                       ciclo_adimplencia,
                       status_adimplencia,
                       ativo
                FROM cliente
                WHERE id = ?
                  AND locadora_id = ?
                FOR UPDATE
                """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new ClienteCiclo(
                        rs.getLong("id"),
                        rs.getInt("ciclo_adimplencia"),
                        StatusAdimplencia.valueOf(
                                rs.getString("status_adimplencia")),
                        rs.getBoolean("ativo")
                ),
                clienteId,
                locadoraId
        ).stream().findFirst();
    }

    public boolean gerenteAutorizado(
            Long gerenteId,
            Long locadoraId) {

        String sql = """
                SELECT id
                FROM usuario
                WHERE id = ?
                  AND locadora_id = ?
                  AND perfil = 'GERENTE'
                  AND status = 'ATIVO'
                FOR SHARE
                """;

        return !jdbcTemplate.query(
                sql,
                (rs, rowNum) -> rs.getLong("id"),
                gerenteId,
                locadoraId
        ).isEmpty();
    }

    public Long registrarDecisao(
            Long locadoraId,
            Long clienteId,
            Long gerenteId,
            Integer cicloAnterior,
            DecisaoReconsideracao decisao,
            String justificativa,
            Instant dataDecisao) {

        String sql = """
                INSERT INTO reconsideracao_cliente (
                    locadora_id,
                    cliente_id,
                    gerente_id,
                    ciclo_anterior,
                    decisao,
                    justificativa,
                    data_decisao
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """;

        return jdbcTemplate.queryForObject(
                sql,
                Long.class,
                locadoraId,
                clienteId,
                gerenteId,
                cicloAnterior,
                decisao.name(),
                justificativa,
                java.sql.Timestamp.from(dataDecisao)
        );
    }

    public int iniciarNovoCiclo(
            Long clienteId,
            Long locadoraId,
            Integer cicloAnterior) {

        String sql = """
                UPDATE cliente
                SET status_adimplencia = 'ADIMPLENTE',
                    ciclo_adimplencia = ciclo_adimplencia + 1
                WHERE id = ?
                  AND locadora_id = ?
                  AND ciclo_adimplencia = ?
                  AND status_adimplencia = 'INADIMPLENTE'
                """;

        return jdbcTemplate.update(
                sql,
                clienteId,
                locadoraId,
                cicloAnterior
        );
    }
}