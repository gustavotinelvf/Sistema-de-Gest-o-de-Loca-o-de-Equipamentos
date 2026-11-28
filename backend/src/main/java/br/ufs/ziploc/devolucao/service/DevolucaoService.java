package br.ufs.ziploc.devolucao.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.ufs.ziploc.devolucao.domain.Devolucao;
import br.ufs.ziploc.devolucao.dto.DevolucaoDto;
import br.ufs.ziploc.devolucao.dto.RegistrarDevolucaoDto;
import br.ufs.ziploc.devolucao.exceptions.ItemContratoNaoEncontradoException;
import br.ufs.ziploc.devolucao.repository.DevolucaoRepository;
import br.ufs.ziploc.faturamento.api.FaturaApi;
import br.ufs.ziploc.locacao.api.ContratoApi;
import br.ufs.ziploc.locacao.api.ContratoNaoEncontradoException;
import br.ufs.ziploc.locacao.api.ContratoResumo;
import br.ufs.ziploc.locacao.api.ItemContratoPrazo;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor 

public class DevolucaoService {

    private final DevolucaoRepository devolucaoRepository;
    private final ContratoApi contratoApi;
    private final FaturaApi faturaApi;
    private static final ZoneId FUSO_LOCAL = ZoneId.of("America/Sao_Paulo");

    @Transactional 
    public DevolucaoDto registrar(RegistrarDevolucaoDto request, Long locadoraId, Long usuarioId ){
        ItemContratoPrazo itemContrato = contratoApi.buscarItemContrato(request.itemContratoId()).orElseThrow(
            () ->new ItemContratoNaoEncontradoException(request.itemContratoId())
        );

         /*trocar por Clock quando houver testes da multa */
        Instant dataDevolucao = Instant.now();
        LocalDate diaDevolucao = dataDevolucao.atZone(FUSO_LOCAL).toLocalDate();

        BigDecimal multaFinal;

        if(diaDevolucao.isAfter(itemContrato.dataFimPrevista())){

            long diasDeAtraso = ChronoUnit.DAYS.between(itemContrato.dataFimPrevista(), diaDevolucao);
            multaFinal = calcularMultaAtraso(itemContrato, diasDeAtraso);
        }
        else{
            multaFinal = BigDecimal.ZERO;
        }

        /*Devolve o contrato inteiro, com valorTotal e a lista de itens */
        ContratoResumo contrato = contratoApi.buscarContrato(itemContrato.contratoId()).orElseThrow(
            () -> new ContratoNaoEncontradoException("Contrato "+ itemContrato.contratoId() + " não encontrado")
        );

        /*Calcula o valor máximo que a multa pode assumir sem ultrapassar o teto, que é o contrato */
        BigDecimal multaLimitada = contrato.valorTotal()
        .subtract(somarMultasRegistradas(contrato)).min(multaFinal).max(BigDecimal.ZERO);

        Devolucao devolucao = Devolucao.builder()
        .locadoraId(locadoraId)
        .itemContratoId(request.itemContratoId())
        .usuarioId(usuarioId)
        .dataDevolucaoReal(dataDevolucao)
        .multaAtraso(multaLimitada)
        .observacoes(request.observacoes())
        .build();

        devolucaoRepository.save(devolucao);

        if(contratoApi.contarItensPendentes(itemContrato.contratoId()) == 0){
            
            BigDecimal somaDasMultas = somarMultasRegistradas(contrato);
            
            faturaApi.gerarFatura(itemContrato.contratoId(),
             locadoraId, usuarioId, somaDasMultas);
        }

        return DevolucaoDto.recebimentoDevolucao(devolucao);
    }

    private BigDecimal calcularMultaAtraso(ItemContratoPrazo itemContrato, long diasAtraso){
        
        if(diasAtraso <=0){
            return BigDecimal.ZERO;
        }
        
        BigDecimal percentualMulta = itemContrato.percentualMulta()
        .divide(BigDecimal.valueOf(100), MathContext.DECIMAL128);

        BigDecimal multa = BigDecimal.valueOf(diasAtraso)
        .multiply(itemContrato.valorDiariaContratada())
        .multiply(percentualMulta);
        
        return multa.setScale(2,RoundingMode.HALF_UP);
    }

    private BigDecimal somarMultasRegistradas(ContratoResumo contratoResumo){
        
        List<Long> ids = contratoResumo.itens().stream().map(ItemContratoPrazo::id).toList();

        List<Devolucao> devolucoes = devolucaoRepository.findByItemContratoIdIn(ids);

        return devolucoes.stream()
            .map(Devolucao::getMultaAtraso)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

}
