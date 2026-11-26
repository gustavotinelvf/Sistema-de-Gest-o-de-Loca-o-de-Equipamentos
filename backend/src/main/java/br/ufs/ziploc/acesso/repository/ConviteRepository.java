package br.ufs.ziploc.acesso.repository;

import br.ufs.ziploc.acesso.domain.Convite;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConviteRepository extends JpaRepository<Convite, Long> {

    /** O link do e-mail traz o token em texto: o service calcula o hash e busca por ele. */
    Optional<Convite> findByTokenHash(String tokenHash);

    /** Convites ainda PENDENTES cuja data de expiração já passou (rotina da RD017). */
    @Query("""
            select c from Convite c
            where c.status = br.ufs.ziploc.acesso.domain.StatusConvite.PENDENTE
              and c.dataExpiracao <= :agora
            """)
    List<Convite> findPendentesVencidos(@Param("agora") Instant agora);
}
