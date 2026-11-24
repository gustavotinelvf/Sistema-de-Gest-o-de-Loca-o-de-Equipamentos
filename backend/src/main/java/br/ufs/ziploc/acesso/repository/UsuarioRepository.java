package br.ufs.ziploc.acesso.repository;

import br.ufs.ziploc.acesso.domain.Perfil;
import br.ufs.ziploc.acesso.domain.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Só salva e busca dados: nenhuma regra de negócio aqui (seção 11). */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findByCpf(String cpf);

    boolean existsByEmail(String email);

    /** Usado pela carga inicial: "só cria o primeiro Gerente se não existir nenhum". */
    boolean existsByPerfil(Perfil perfil);
}
