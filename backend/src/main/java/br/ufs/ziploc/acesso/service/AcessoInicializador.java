package br.ufs.ziploc.acesso.service;

import br.ufs.ziploc.acesso.domain.EnderecoLocadora;
import br.ufs.ziploc.acesso.domain.Locadora;
import br.ufs.ziploc.acesso.domain.Perfil;
import br.ufs.ziploc.acesso.domain.Usuario;
import br.ufs.ziploc.acesso.repository.LocadoraRepository;
import br.ufs.ziploc.acesso.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carga inicial (sem rota): na subida do sistema, se ainda não existe nenhum
 * Gerente, cria o primeiro Gerente e a linha única da locadora a partir de
 * variáveis de ambiente. Nada disso vai para o Git. Os demais Gerentes
 * entram por convite (CPF obrigatório, RD016).
 *
 * Variáveis obrigatórias: ADMIN_NOME, ADMIN_EMAIL, ADMIN_CPF, ADMIN_SENHA,
 * LOCADORA_RAZAO_SOCIAL, LOCADORA_CNPJ, LOCADORA_CEP, LOCADORA_LOGRADOURO,
 * LOCADORA_NUMERO, LOCADORA_BAIRRO, LOCADORA_CIDADE, LOCADORA_UF,
 * LOCADORA_RAIO_ENTREGA_KM, LOCADORA_PERCENTUAL_MULTA.
 * Opcional: LOCADORA_COMPLEMENTO.
 *
 * Se faltar alguma, o sistema sobe mesmo assim e registra um aviso no log
 * dizendo quais faltam (nada é criado).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AcessoInicializador implements ApplicationRunner {

    private static final List<String> OBRIGATORIAS = List.of(
            "ADMIN_NOME", "ADMIN_EMAIL", "ADMIN_CPF", "ADMIN_SENHA",
            "LOCADORA_RAZAO_SOCIAL", "LOCADORA_CNPJ", "LOCADORA_CEP",
            "LOCADORA_LOGRADOURO", "LOCADORA_NUMERO", "LOCADORA_BAIRRO",
            "LOCADORA_CIDADE", "LOCADORA_UF",
            "LOCADORA_RAIO_ENTREGA_KM", "LOCADORA_PERCENTUAL_MULTA");

    private final UsuarioRepository usuarioRepository;
    private final LocadoraRepository locadoraRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment env;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (usuarioRepository.existsByPerfil(Perfil.GERENTE)) {
            return;
        }

        List<String> faltando = new ArrayList<>();
        for (String nome : OBRIGATORIAS) {
            String valor = env.getProperty(nome);
            if (valor == null || valor.isBlank()) {
                faltando.add(nome);
            }
        }
        if (!faltando.isEmpty()) {
            log.warn("Carga inicial não executada: não existe Gerente e faltam as variáveis {}", faltando);
            return;
        }

        // No schema do Michael todo usuário pertence a uma locadora (locadora_id NOT NULL).
        Locadora locadora = locadoraRepository.findUnica()
                .orElseGet(() -> locadoraRepository.save(montarLocadora()));

        Usuario gerente = Usuario.criarAtivo(
                locadora.getId(),
                env.getProperty("ADMIN_NOME").trim(),
                env.getProperty("ADMIN_EMAIL").trim(),
                passwordEncoder.encode(env.getProperty("ADMIN_SENHA")),
                Perfil.GERENTE,
                apenasDigitos(env.getProperty("ADMIN_CPF")));
        usuarioRepository.save(gerente);
        log.info("Carga inicial executada: primeiro Gerente criado ({})", gerente.getEmail());
    }

    private Locadora montarLocadora() {
        String complemento = env.getProperty("LOCADORA_COMPLEMENTO");
        EnderecoLocadora endereco = new EnderecoLocadora(
                apenasDigitos(env.getProperty("LOCADORA_CEP")),
                env.getProperty("LOCADORA_LOGRADOURO").trim(),
                env.getProperty("LOCADORA_NUMERO").trim(),
                (complemento == null || complemento.isBlank()) ? null : complemento.trim(),
                env.getProperty("LOCADORA_BAIRRO").trim(),
                env.getProperty("LOCADORA_CIDADE").trim(),
                env.getProperty("LOCADORA_UF").trim().toUpperCase());
        return new Locadora(
                env.getProperty("LOCADORA_RAZAO_SOCIAL").trim(),
                apenasDigitos(env.getProperty("LOCADORA_CNPJ")),
                endereco,
                new BigDecimal(env.getProperty("LOCADORA_RAIO_ENTREGA_KM").trim()),
                new BigDecimal(env.getProperty("LOCADORA_PERCENTUAL_MULTA").trim()));
    }

    /** O banco exige CPF, CNPJ e CEP só com dígitos. */
    private static String apenasDigitos(String valor) {
        return valor.replaceAll("\\D", "");
    }
}
