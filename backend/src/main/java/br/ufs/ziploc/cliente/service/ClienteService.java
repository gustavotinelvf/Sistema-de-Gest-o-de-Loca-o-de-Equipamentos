package br.ufs.ziploc.cliente.service;

import br.ufs.ziploc.cliente.api.ClienteApi;
import br.ufs.ziploc.cliente.api.ClienteResumoDTO;
import br.ufs.ziploc.cliente.domain.Cliente;
import br.ufs.ziploc.cliente.domain.Contato;
import br.ufs.ziploc.cliente.domain.Endereco;
import br.ufs.ziploc.cliente.domain.StatusAdimplencia;
import br.ufs.ziploc.cliente.repository.ClienteRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClienteService implements ClienteApi {

    private final ClienteRepository clienteRepository;

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
                .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado com id: " + clienteId));
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