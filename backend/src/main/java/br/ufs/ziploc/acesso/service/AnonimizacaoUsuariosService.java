package br.ufs.ziploc.acesso.service;

import br.ufs.ziploc.acesso.domain.Usuario;
import br.ufs.ziploc.acesso.repository.UsuarioRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Limpeza LGPD de usuários inativos (tarefa 18; rotina EXTRA validada pelo Tech Lead).
 * O prazo de retenção não está no Relatório: é configurável (ziploc.lgpd.usuarios-inativos-dias,
 * padrão 365) e precisa ser confirmado.
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
        this.usuarioRepository = usuarioRepository;
        this.diasRetencao = diasRetencao;
    }

    /** Anonimiza os inativos mais antigos que o prazo e devolve quantos foram anonimizados. */
    @Transactional
    public int anonimizarInativos(Instant agora) {
        Instant limite = agora.minus(diasRetencao, ChronoUnit.DAYS);
        List<Usuario> candidatos = usuarioRepository.findInativosParaAnonimizacao(limite);
        candidatos.forEach(usuario -> usuario.anonimizar(agora));
        usuarioRepository.saveAll(candidatos);
        return candidatos.size();
    }
}
