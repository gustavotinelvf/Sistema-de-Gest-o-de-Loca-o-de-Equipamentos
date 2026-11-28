package br.ufs.ziploc.acesso.service;

import br.ufs.ziploc.acesso.domain.Usuario;
import br.ufs.ziploc.acesso.repository.UsuarioRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Limpeza LGPD de usuários inativos (tarefa 18; rotina EXTRA validada pelo Tech Lead).
 * O prazo de retenção é contado a partir da data de inativação do usuário.
 * Configurável em ziploc.lgpd.usuarios-inativos-dias, com padrão de 365 dias.
 */
@Service
public class AnonimizacaoUsuariosService {

    private final UsuarioRepository usuarioRepository;
    private final long diasRetencao;

    public AnonimizacaoUsuariosService(
            UsuarioRepository usuarioRepository,
            @Value("${ziploc.lgpd.usuarios-inativos-dias:365}") long diasRetencao) {

        if (diasRetencao <= 0) {
            throw new IllegalArgumentException("O prazo de retenção deve ser maior que zero");
        }

        this.usuarioRepository = Objects.requireNonNull(usuarioRepository,
                "O repositório de usuários é obrigatório");
        this.diasRetencao = diasRetencao;
    }

    /** Anonimiza os usuários que atingiram o prazo de retenção. */
    @Transactional
    public int anonimizarInativos(Instant agora) {
        Objects.requireNonNull(agora, "A data de referência é obrigatória");

        Instant limite = agora.minus(diasRetencao, ChronoUnit.DAYS);
        List<Usuario> candidatos = usuarioRepository.findInativosParaAnonimizacao(limite);

        if (candidatos.isEmpty()) {
            return 0;
        }

        candidatos.forEach(usuario -> usuario.anonimizar(agora));
        usuarioRepository.saveAll(candidatos);

        return candidatos.size();
    }
}