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
import br.ufs.ziploc.shared.exception.ConflitoException;
import br.ufs.ziploc.shared.exception.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClienteService implements ClienteApi {

    private final ClienteRepository clienteRepository;
    private final LocadoraApi locadoraApi;

    @Transactional
    public ClienteResponseDTO cadastrar(ClienteRequestDTO request) {
        String cnpjLimpo = CnpjValidador.limpar(request.cnpj());
        if (!CnpjValidador.ehValido(cnpjLimpo)) {
            throw new IllegalArgumentException("CNPJ inválido.");
        }
        if (clienteRepository.existsByCnpj(cnpjLimpo)) {
            throw new ConflitoException("Já existe um cliente cadastrado com o CNPJ informado.");
        }

        Long locadoraId = locadoraApi.obterParametros().id();
        Contato contato = montarContato(request);
        Endereco endereco = montarEndereco(request);

        Cliente cliente = new Cliente(locadoraId, request.razaoSocial(), cnpjLimpo, contato, endereco);
        return ClienteResponseDTO.de(clienteRepository.save(cliente));
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
        return cliente.getStatusAdimplencia() == StatusAdimplencia.ADIMPLENTE;
    }

    @Override
    @Transactional
    public void alterarStatusAdimplencia(Long clienteId, StatusAdimplencia status) {
        Cliente cliente = buscarPorIdOuFalhar(clienteId);
        cliente.alterarStatusAdimplencia(status);
        clienteRepository.save(cliente);
    }

    private Cliente buscarPorIdOuFalhar(Long clienteId) {
        return clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado com id: " + clienteId));
    }

    private Contato montarContato(ClienteRequestDTO request) {
        String nome = normalizarOpcional(request.representanteNome());
        String cpf = normalizarOpcional(request.representanteCpf());
        String email = normalizarOpcional(request.email());
        String telefone = normalizarOpcional(request.telefone());

        if (nome == null && cpf == null && email == null && telefone == null) {
            return null;
        }
        return new Contato(nome, cpf, email, telefone);
    }

    private Endereco montarEndereco(ClienteRequestDTO request) {
        String cepLimpo = request.cep().replaceAll("\\D", "");
        return new Endereco(
                cepLimpo,
                request.logradouro().trim(),
                request.numero().trim(),
                normalizarOpcional(request.complemento()),
                request.bairro().trim(),
                request.cidade().trim(),
                request.uf().trim().toUpperCase()
        );
    }

    private String normalizarOpcional(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
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