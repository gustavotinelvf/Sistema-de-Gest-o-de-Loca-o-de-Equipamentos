package br.ufs.ziploc.cliente.service;

import br.ufs.ziploc.cliente.dto.CepResponseDTO;
import br.ufs.ziploc.integracao.cep.CepGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Consulta de CEP separada do cadastro (decisão do Tech Lead): o cadastro não depende dela. */
@Service
@RequiredArgsConstructor
public class ConsultaCepService {

    private final CepGateway cepGateway;

    public CepResponseDTO consultar(String cep) {
        String digitos = cep == null ? "" : cep.replaceAll("\\D", "");
        if (digitos.length() != 8) {
            throw new CepInvalidoException();
        }
        return cepGateway.consultar(digitos)
                .map(CepResponseDTO::from)
                .orElseThrow(CepNaoEncontradoException::new);
    }
}
