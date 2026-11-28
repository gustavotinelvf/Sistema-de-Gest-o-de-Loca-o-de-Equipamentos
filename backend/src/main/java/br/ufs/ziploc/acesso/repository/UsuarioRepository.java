package br.ufs.ziploc.acesso.repository;

import br.ufs.ziploc.acesso.domain.Perfil;
import br.ufs.ziploc.acesso.domain.Usuario;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Só salva e busca dados: nenhuma regra de negócio aqui (seção 11). */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findByCpf(String cpf);

    boolean existsByEmail(String email);

    /** Usado pela carga inicial: "só cria o primeiro Gerente se não existir nenhum". */
    boolean existsByPerfil(Perfil perfil);

    /**
     * Candidatos à anonimização LGPD: INATIVOS, ainda não anonimizados, cadastrados até o limite.
     * Não há coluna com a data da inativação; usa-se data_cadastro (mesmo critério do cliente).
     */
    @Query("""
            SELECT u FROM Usuario u
            WHERE u.status = br.ufs.ziploc.acesso.domain.StatusUsuario.INATIVO
              AND u.anonimizadoEm IS NULL
              AND u.dataCadastro <= :limite
            """)
    List<Usuario> findInativosParaAnonimizacao(@Param("limite") Instant limite);
}
