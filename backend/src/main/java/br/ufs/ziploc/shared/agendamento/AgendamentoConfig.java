package br.ufs.ziploc.shared.agendamento;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Liga o suporte a @Scheduled na aplicação (sem isto, as rotinas agendadas nunca rodam). */
@Configuration
@EnableScheduling
public class AgendamentoConfig {
}
