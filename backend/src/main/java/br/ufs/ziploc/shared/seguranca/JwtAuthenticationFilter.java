package br.ufs.ziploc.shared.seguranca;

import br.ufs.ziploc.acesso.api.UsuarioApi;
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
 * Valida o JWT e consulta o estado atual do usuário.
 * authentication.getName() = id do usuário (texto).
 * authorities = ROLE_<PERFIL>.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIXO = "Bearer ";

    private final JwtService jwtService;
    private final UsuarioApi usuarioApi;

    public JwtAuthenticationFilter(JwtService jwtService, UsuarioApi usuarioApi) {
        this.jwtService = jwtService;
        this.usuarioApi = usuarioApi;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String cabecalho = request.getHeader("Authorization");
        if (cabecalho != null && cabecalho.startsWith(PREFIXO)) {
            jwtService.validar(cabecalho.substring(PREFIXO.length())).ifPresent(dados ->
                    usuarioApi.consultarPerfilAtivo(dados.usuarioId()).ifPresent(perfil -> {
                        var autenticacao = new UsernamePasswordAuthenticationToken(
                                String.valueOf(dados.usuarioId()),
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + perfil)));
                        SecurityContextHolder.getContext().setAuthentication(autenticacao);
                    }));
        }
        chain.doFilter(request, response);
    }
}
