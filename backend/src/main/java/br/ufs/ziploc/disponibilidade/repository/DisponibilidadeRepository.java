package br.ufs.ziploc.disponibilidade.repository;

import br.ufs.ziploc.disponibilidade.api.DisponibilidadeDTO;
import br.ufs.ziploc.disponibilidade.api.UnidadeDTO;
import br.ufs.ziploc.disponibilidade.domain.Periodo;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class DisponibilidadeRepository {
    private static final String UNIDADE_LIVRE = """
            u.ativo AND m.ativo
            AND u.status <> 'EM_MANUTENCAO'
            AND (u.status <> 'EM_VISTORIA' OR :inicio > CURRENT_DATE + 2)
            AND NOT EXISTS (SELECT 1 FROM item_contrato ic WHERE ic.unidade_id = u.id
                AND ic.data_inicio_prevista <= :fim AND ic.data_fim_prevista >= :inicio)
            AND NOT EXISTS (SELECT 1 FROM item_contrato ic WHERE ic.unidade_id = u.id
                AND ic.data_inicio_real IS NOT NULL AND ic.data_devolucao_real IS NULL AND ic.data_fim_prevista < CURRENT_DATE)
            """;
    private final NamedParameterJdbcTemplate jdbc;

    public List<DisponibilidadeDTO> contarLivresPorModelo(Periodo periodo, String categoria) {
        String sql = """
                SELECT m.id, m.nome, m.categoria, m.valor_diaria_base, COUNT(u.id) AS quantidade
                FROM modelo_equipamento m JOIN unidade_equipamento u ON u.modelo_id = m.id
                WHERE (CAST(:categoria AS VARCHAR) IS NULL OR m.categoria = :categoria) AND\s""" + UNIDADE_LIVRE + """
                GROUP BY m.id, m.nome, m.categoria, m.valor_diaria_base
                ORDER BY m.nome, m.id
                """;
        return jdbc.query(sql, parametros(periodo).addValue("categoria", categoria), (rs, i) -> new DisponibilidadeDTO(rs.getLong("id"), rs.getString("nome"), rs.getString("categoria"), rs.getBigDecimal("valor_diaria_base"), rs.getInt("quantidade")));
    }

    public void travarUnidadesDosModelos(Collection<Long> modeloIds) {
        jdbc.queryForList("SELECT u.id FROM unidade_equipamento u WHERE u.modelo_id IN (:modelos) ORDER BY u.id FOR UPDATE", new MapSqlParameterSource("modelos", modeloIds), Long.class);
    }

    public List<UnidadeDTO> buscarUnidadesLivres(Collection<Long> modeloIds, Periodo periodo) {
        String sql = """
                SELECT u.id, u.modelo_id, m.valor_diaria_base
                FROM unidade_equipamento u JOIN modelo_equipamento m ON m.id = u.modelo_id
                WHERE u.modelo_id IN (:modelos) AND\s""" + UNIDADE_LIVRE + """
                ORDER BY u.id
                """;
        return jdbc.query(sql, parametros(periodo).addValue("modelos", modeloIds), (rs, i) -> new UnidadeDTO(rs.getLong("id"), rs.getLong("modelo_id"), rs.getBigDecimal("valor_diaria_base")));
    }

    private static MapSqlParameterSource parametros(Periodo periodo) {
        return new MapSqlParameterSource().addValue("inicio", periodo.inicio()).addValue("fim", periodo.fim());
    }
}
