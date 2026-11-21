package br.ufs.ziploc.manutencao.fake;

import br.ufs.ziploc.equipamento.api.UnidadeApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

// TODO: NOMES A SEREM SUBSTITUÍDOS A POSTERIORI, NOMES TEMPORÁRIOS PARA VER SE TÁ FUNCIONANDO NUM TESTE

@Component
@Profile("fake")
class UnidadeApiFake implements UnidadeApi {
    private static final Logger log = LoggerFactory.getLogger(UnidadeApiFake.class);

    @Override
    public void alterarStatus(Long unidadeId, String novoStatus) {
        log.info("[FAKE] unidade {} -> {}", unidadeId, novoStatus);
    }
}