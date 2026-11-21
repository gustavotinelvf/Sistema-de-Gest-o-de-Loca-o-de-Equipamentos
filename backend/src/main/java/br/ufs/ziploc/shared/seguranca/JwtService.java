package br.ufs.ziploc.shared.seguranca;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Emite e valida o JWT sem estado (seção 12 do Relatório).
 * O segredo vem da variável de ambiente JWT_SECRET (nunca vai para o Git).
 * Validade curta: proposta de 2 horas, sem renovação.
 */
@Component
public class JwtService {

    private static final String CLAIM_PERFIL = "perfil";

    private final SecretKey chave;
    private final long expiracaoMinutos;

    public JwtService(@Value("${JWT_SECRET}") String segredo,
                      @Value("${JWT_EXPIRACAO_MINUTOS:120}") long expiracaoMinutos) {
        // HS256 exige pelo menos 32 caracteres (256 bits).
        this.chave = Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8));
        this.expiracaoMinutos = expiracaoMinutos;
    }

    public TokenEmitido gerar(Long usuarioId, String perfil) {
        Instant agora = Instant.now();
        Instant expiraEm = agora.plus(Duration.ofMinutes(expiracaoMinutos));
        String token = Jwts.builder()
                .subject(String.valueOf(usuarioId))
                .claim(CLAIM_PERFIL, perfil)
                .issuedAt(Date.from(agora))
                .expiration(Date.from(expiraEm))
                .signWith(chave)
                .compact();
        return new TokenEmitido(token, expiraEm);
    }

    /** Devolve os dados do token, ou vazio se ele for inválido, adulterado ou expirado. */
    public Optional<DadosToken> validar(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(chave)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(new DadosToken(
                    Long.valueOf(claims.getSubject()),
                    claims.get(CLAIM_PERFIL, String.class)));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public record TokenEmitido(String token, Instant expiraEm) {
    }

    public record DadosToken(Long usuarioId, String perfil) {
    }
}
