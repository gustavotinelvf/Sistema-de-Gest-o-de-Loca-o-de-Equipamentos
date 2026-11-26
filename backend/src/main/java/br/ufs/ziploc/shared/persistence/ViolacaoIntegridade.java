package br.ufs.ziploc.shared.persistence;

import java.sql.SQLException;

/** Identifica o tipo de violação de integridade do PostgreSQL pelo SQLState, olhando toda a cadeia de causas. */
public final class ViolacaoIntegridade {
    /** unique_violation (ex.: uk_unidade_equipamento_numero_serie). */
    public static final String UNICA = "23505";
    /** foreign_key_violation. */
    public static final String CHAVE_ESTRANGEIRA = "23503";
    /** exclusion_violation (ex.: ex_item_contrato_sem_overbooking). */
    public static final String EXCLUSAO = "23P01";

    private ViolacaoIntegridade() {
    }

    public static boolean temSqlState(Throwable erro, String sqlState) {
        for (Throwable t = erro; t != null; t = t.getCause() == t ? null : t.getCause()) {
            if (t instanceof SQLException sql && sqlState.equals(sql.getSQLState())) return true;
        }
        return false;
    }
}
