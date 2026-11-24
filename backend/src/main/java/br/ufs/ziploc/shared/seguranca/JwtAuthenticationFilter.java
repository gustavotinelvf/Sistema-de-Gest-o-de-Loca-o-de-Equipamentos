package br.ufs.ziploc.shared.seguranca;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Lê "Authorization: Bearer <token>", valida o JWT e coloca o usuário no contexto.
 * Contrato que os outros módulos já assumem:
 *  - authentication.getName() = id do usuário (texto);
 *  - authorities = ROLE_<PERFIL> (ex.: ROLE_TECNICO, para hasRole('TECNICO')).
 * Token ausente ou inválido: o filtro segue sem autenticar e o Spring responde 401.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIXO = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String cabecalho = request.getHeader("Authorization");
        if (cabecalho != null && cabecalho.startsWith(PREFIXO)) {
            jwtService.validar(cabecalho.substring(PREFIXO.length())).ifPresent(dados -> {
                var autenticacao = new UsernamePasswordAuthenticationToken(
                        String.valueOf(dados.usuarioId()),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + dados.perfil())));
                SecurityContextHolder.getContext().setAuthentication(autenticacao);
            });
        }
        chain.doFilter(request, response);
    }
}
