package br.ufs.ziploc.cliente.service;

import br.ufs.ziploc.cliente.domain.DecisaoReconsideracao;
import br.ufs.ziploc.cliente.domain.StatusAdimplencia;
import br.ufs.ziploc.cliente.dto.RegistrarReconsideracaoDTO;
import br.ufs.ziploc.cliente.dto.ReconsideracaoResponseDTO;
import br.ufs.ziploc.cliente.repository.ReconsideracaoRepository;
import br.ufs.ziploc.shared.exception.ConflitoException;
import br.ufs.ziploc.shared.exception.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ReconsideracaoService {

    private final ReconsideracaoRepository reconsideracoes;

    @Transactional
    public ReconsideracaoResponseDTO registrar(
            Long clienteId,
            Long locadoraId,
            Long gerenteId,
            RegistrarReconsideracaoDTO request) {

        validarIdentificador(clienteId, "Cliente");
        validarIdentificador(locadoraId, "Locadora");
        validarIdentificador(gerenteId, "Gerente");

        if (request == null || request.decisao() == null) {

            throw new IllegalArgumentException(
                    "A decisão da reconsideração é obrigatória.");

        }

        String justificativa =
                validarJustificativa(request.justificativa());

        // Verifica o perfil e o vínculo atual do gerente.

        if (!reconsideracoes.gerenteAutorizado(
                gerenteId, locadoraId)) {

            throw new AccessDeniedException(
                    "Somente gerentes ativos da locadora "
                            + "podem registrar reconsiderações.");

        }

        // Bloqueia o cliente durante toda a operação.

        ReconsideracaoRepository.ClienteCiclo cliente =
                reconsideracoes.buscarClienteParaAtualizacao(
                                clienteId, locadoraId)
                        .orElseThrow(() ->
                                new RecursoNaoEncontradoException(
                                        "Cliente não encontrado."));

        if (!cliente.ativo()) {

            throw new ConflitoException(
                    "Não é possível reconsiderar um cliente inativo.");

        }

        if (cliente.status() != StatusAdimplencia.INADIMPLENTE) {

            throw new ConflitoException(
                    "Somente clientes inadimplentes "
                            + "podem passar por reconsideração.");

        }

        Instant dataDecisao = Instant.now();

        Integer cicloAnterior = cliente.ciclo();

        Integer cicloAtual = cicloAnterior;

        StatusAdimplencia statusAtual = cliente.status();

        // Preserva decisões aprovadas e rejeitadas.

        Long reconsideracaoId =
                reconsideracoes.registrarDecisao(
                        locadoraId,
                        clienteId,
                        gerenteId,
                        cicloAnterior,
                        request.decisao(),
                        justificativa,
                        dataDecisao
                );

        // Somente a aprovação inicia um novo ciclo.

        if (request.decisao()
                == DecisaoReconsideracao.APROVADA) {

            int atualizados =
                    reconsideracoes.iniciarNovoCiclo(
                            clienteId,
                            locadoraId,
                            cicloAnterior
                    );

            if (atualizados != 1) {

                throw new ConflitoException(
                        "Não foi possível iniciar "
                                + "um novo ciclo de adimplência.");

            }

            cicloAtual = Math.addExact(cicloAnterior, 1);

            statusAtual = StatusAdimplencia.ADIMPLENTE;

        }

        return new ReconsideracaoResponseDTO(
                reconsideracaoId,
                clienteId,
                gerenteId,
                request.decisao(),
                justificativa,
                dataDecisao,
                cicloAnterior,
                cicloAtual,
                statusAtual
        );
    }

    private static void validarIdentificador(
            Long id,
            String campo) {

        if (id == null || id <= 0) {

            throw new IllegalArgumentException(
                    campo + " possui identificador inválido.");

        }
    }

    private static String validarJustificativa(
            String justificativa) {

        if (justificativa == null
                || justificativa.isBlank()) {

            throw new IllegalArgumentException(
                    "A justificativa é obrigatória.");

        }

        String normalizada = justificativa.strip();

        if (normalizada.length() > 2000) {

            throw new IllegalArgumentException(
                    "A justificativa deve possuir "
                            + "no máximo 2000 caracteres.");

        }

        return normalizada;
    }
}