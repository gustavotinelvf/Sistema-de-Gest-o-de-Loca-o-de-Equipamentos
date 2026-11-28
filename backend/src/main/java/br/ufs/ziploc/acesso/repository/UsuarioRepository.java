package br.ufs.ziploc.acesso.repository;

import br.ufs.ziploc.acesso.domain.Perfil;
import br.ufs.ziploc.acesso.domain.Usuario;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Só salva e busca dados: nenhuma regra de negócio aqui (seção 11). */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findByCpf(String cpf);

    boolean existsByEmail(String email);

    /** Usado pela carga inicial: "só cria o primeiro Gerente se não existir nenhum". */
    boolean existsByPerfil(Perfil perfil);

    /** Busca um usuário com bloqueio para operações que alteram seu estado. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM Usuario u WHERE u.id = :id")
    Optional<Usuario> findByIdForUpdate(@Param("id") Long id);

    /**
     * Candidatos à anonimização LGPD: usuários INATIVOS, ainda não
     * anonimizados, cujo período de retenção foi atingido.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT u FROM Usuario u
            WHERE u.status = br.ufs.ziploc.acesso.domain.StatusUsuario.INATIVO
              AND u.anonimizadoEm IS NULL
              AND u.dataInativacao <= :limite
            ORDER BY u.id ASC
            """)
    List<Usuario> findInativosParaAnonimizacao(@Param("limite") Instant limite);
}