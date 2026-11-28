package br.ufs.ziploc.acesso.service;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Dispara a limpeza LGPD todo dia às 03:00 (configurável em ziploc.lgpd.cron). Não registra dados pessoais. */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnonimizacaoUsuariosAgendador {

    private final AnonimizacaoUsuariosService service;

    @Scheduled(cron = "${ziploc.lgpd.cron:0 0 3 * * *}")
    public void executar() {
        int total = service.anonimizarInativos(Instant.now());
        log.info("Limpeza LGPD de usuários: {} anonimizado(s)", total);
    }
}
