package br.ufs.ziploc.manutencao.provisorio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** TODO: PROVISÓRIO. Apagar junto com a interface quando a DevolucaoApi oficial tiver registrarDanos. */
@Component
class DevolucaoApiProvisoria implements DevolucaoApi {

    private static final Logger log = LoggerFactory.getLogger(DevolucaoApiProvisoria.class);

    @Override
    public void registrarDanos(Long unidadeId, BigDecimal valor) {
        log.warn("[PROVISORIO] danos NAO repassados a Devolucao: unidade {} valor {}", unidadeId, valor);
    }
}