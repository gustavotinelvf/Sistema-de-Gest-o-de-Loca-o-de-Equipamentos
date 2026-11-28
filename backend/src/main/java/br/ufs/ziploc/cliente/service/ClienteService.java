package br.ufs.ziploc.cliente.service;

import br.ufs.ziploc.acesso.api.LocadoraApi;
import br.ufs.ziploc.cliente.api.ClienteApi;
import br.ufs.ziploc.cliente.api.ClienteResumoDTO;
import br.ufs.ziploc.cliente.domain.Cliente;
import br.ufs.ziploc.cliente.domain.CnpjValidador;
import br.ufs.ziploc.cliente.domain.Contato;
import br.ufs.ziploc.cliente.domain.Endereco;
import br.ufs.ziploc.cliente.domain.StatusAdimplencia;
import br.ufs.ziploc.cliente.dto.ClienteRequestDTO;
import br.ufs.ziploc.cliente.dto.ClienteResponseDTO;
import br.ufs.ziploc.cliente.repository.ClienteRepository;
import br.ufs.ziploc.cliente.repository.ClienteSpecs;
import br.ufs.ziploc.shared.exception.ConflitoException;
import br.ufs.ziploc.shared.exception.RecursoNaoEncontradoException;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClienteService implements ClienteApi {

    private static final String CNPJ_DUPLICADO =
            "Já existe um cliente cadastrado com o CNPJ informado.";

    private final ClienteRepository clienteRepository;
    private final LocadoraApi locadoraApi;

    @Transactional
    public ClienteResponseDTO cadastrar(ClienteRequestDTO request) {
        if (!CnpjValidador.ehValido(request.cnpj())) {
            throw new IllegalArgumentException("CNPJ inválido.");
        }

        String cnpjLimpo = CnpjValidador.limpar(request.cnpj());

        if (clienteRepository.existsByCnpj(cnpjLimpo)) {
            throw new ConflitoException(CNPJ_DUPLICADO);
        }

        Long locadoraId = locadoraApi.obterParametros().id();
        Contato contato = montarContato(request);
        Endereco endereco = montarEndereco(request);

        Cliente cliente = new Cliente(
                locadoraId,
                request.razaoSocial(),
                cnpjLimpo,
                contato,
                endereco
        );

        try {
            return ClienteResponseDTO.de(
                    clienteRepository.saveAndFlush(cliente)
            );
        } catch (DataIntegrityViolationException e) {
            if (violouUnicidadeCnpj(e)) {
                throw new ConflitoException(CNPJ_DUPLICADO);
            }
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public Page<ClienteResponseDTO> listar(
            String razaoSocial,
            String cnpj,
            Boolean ativo,
            StatusAdimplencia statusAdimplencia,
            Pageable pageable
    ) {
        Long locadoraId = locadoraApi.obterParametros().id();

        return clienteRepository
                .findAll(
                        ClienteSpecs.comFiltros(
                                locadoraId,
                                razaoSocial,
                                cnpj,
                                ativo,
                                statusAdimplencia
                        ),
                        pageable
                )
                .map(ClienteResponseDTO::de);
    }

    @Transactional(readOnly = true)
    public ClienteResponseDTO buscarPorId(Long id) {
        Long locadoraId = locadoraApi.obterParametros().id();

        Cliente cliente = clienteRepository
                .findByIdAndLocadoraId(id, locadoraId)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException(
                                "Cliente não encontrado com id: " + id
                        ));

        return ClienteResponseDTO.de(cliente);
    }

    @Transactional(readOnly = true)
    public ClienteResponseDTO buscarPorCnpj(String cnpj) {
        if (!CnpjValidador.ehValido(cnpj)) {
            throw new IllegalArgumentException("CNPJ inválido.");
        }

        Long locadoraId = locadoraApi.obterParametros().id();
        String cnpjLimpo = CnpjValidador.limpar(cnpj);

        Cliente cliente = clienteRepository
                .findByCnpjAndLocadoraId(cnpjLimpo, locadoraId)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException(
                                "Cliente não encontrado com CNPJ: " + cnpj
                        ));

        return ClienteResponseDTO.de(cliente);
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResumoDTO consultarResumo(Long clienteId) {
        Cliente cliente = buscarPorIdOuFalhar(clienteId);
        return toResumoDTO(cliente);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean verificarAdimplencia(Long clienteId) {
        Cliente cliente = buscarPorIdOuFalhar(clienteId);

        return cliente.getStatusAdimplencia()
                == StatusAdimplencia.ADIMPLENTE;
    }

    private Cliente buscarPorIdOuFalhar(Long clienteId) {
        return clienteRepository.findById(clienteId)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException(
                                "Cliente não encontrado com id: " + clienteId
                        ));
    }

    private boolean violouUnicidadeCnpj(Throwable erro) {
        Throwable causa = erro;

        while (causa != null) {
            if (causa instanceof ConstraintViolationException violacao
                    && "cliente_cnpj_key".equals(
                            violacao.getConstraintName())) {

                return true;
            }

            causa = causa.getCause();
        }

        return false;
    }

    private Contato montarContato(ClienteRequestDTO request) {
        String nome = normalizarOpcional(request.representanteNome());
        String cpf = normalizarOpcional(request.representanteCpf());
        String email = normalizarOpcional(request.email());
        String telefone = normalizarOpcional(request.telefone());

        if (nome == null
                && cpf == null
                && email == null
                && telefone == null) {

            return null;
        }

        return new Contato(nome, cpf, email, telefone);
    }

    private Endereco montarEndereco(ClienteRequestDTO request) {
        String cepLimpo = request.cep().replace("-", "");

        return new Endereco(
                cepLimpo,
                request.logradouro().trim(),
                request.numero().trim(),
                normalizarOpcional(request.complemento()),
                request.bairro().trim(),
                request.cidade().trim(),
                request.uf().trim().toUpperCase(Locale.ROOT)
        );
    }

    private String normalizarOpcional(String valor) {
        return (valor == null || valor.isBlank())
                ? null
                : valor.trim();
    }

    private ClienteResumoDTO toResumoDTO(Cliente cliente) {
        Contato contato = cliente.getContato();
        Endereco endereco = cliente.getEndereco();

        return new ClienteResumoDTO(
                cliente.getId(),
                cliente.getLocadoraId(),
                cliente.getRazaoSocial(),
                cliente.getCnpj(),
                contato != null ? contato.getNome() : null,
                contato != null ? contato.getCpf() : null,
                contato != null ? contato.getEmail() : null,
                contato != null ? contato.getTelefone() : null,
                cliente.getStatusAdimplencia(),
                cliente.isAtivo(),
                endereco.getCep(),
                endereco.getLogradouro(),
                endereco.getNumero(),
                endereco.getComplemento(),
                endereco.getBairro(),
                endereco.getCidade(),
                endereco.getUf()
        );
    }
}