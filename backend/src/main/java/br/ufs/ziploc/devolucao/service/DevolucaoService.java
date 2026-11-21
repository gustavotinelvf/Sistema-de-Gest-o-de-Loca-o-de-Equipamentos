package br.ufs.ziploc.devolucao.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import br.ufs.ziploc.devolucao.domain.Devolucao;
import br.ufs.ziploc.devolucao.dto.DevolucaoDto;
import br.ufs.ziploc.devolucao.dto.RegistrarDevolucaoDto;
import br.ufs.ziploc.devolucao.exceptions.ItemContratoNaoEncontradoException;
import br.ufs.ziploc.devolucao.provisorio.ClienteApi;
import br.ufs.ziploc.devolucao.provisorio.ContratoApi;
import br.ufs.ziploc.devolucao.provisorio.ItemContratoPrazo;
import br.ufs.ziploc.devolucao.provisorio.StatusUnidade;
import br.ufs.ziploc.devolucao.provisorio.UnidadeApi;
import br.ufs.ziploc.devolucao.repository.DevolucaoRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor 

public class DevolucaoService {

    private final DevolucaoRepository devolucaoRepository;
    private final ContratoApi contratoApi;
    private final UnidadeApi unidadeApi;
    private final ClienteApi clienteApi;
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
        boolean houveAtraso = false;

        if(diaDevolucao.isAfter(itemContrato.dataFimPrevista())){
            houveAtraso = true;

            long diasDeAtraso = ChronoUnit.DAYS.between(itemContrato.dataFimPrevista(), diaDevolucao);
            multaFinal = calcularMultaAtraso(itemContrato, diasDeAtraso);
        }
        else{
            multaFinal = BigDecimal.ZERO;
        }

        Devolucao devolucao = Devolucao.builder()
        .locadoraId(locadoraId)
        .itemContratoId(request.itemContratoId())
        .usuarioId(usuarioId)
        .dataDevolucaoReal(dataDevolucao)
        .multaAtraso(multaFinal)
        .observacoes(request.observacoes())
        .build();

        devolucaoRepository.save(devolucao);

        contratoApi.registrarDevolucao(request.itemContratoId(), dataDevolucao);
        
        unidadeApi.alterarStatus(itemContrato.unidadeId(), StatusUnidade.EM_VISTORIA);
        
        if(houveAtraso){
            clienteApi.registrarAtraso(request.clienteId(), itemContrato.contratoId());
        }

        return DevolucaoDto.recebimentoDevolucao(devolucao);
    }

    private BigDecimal calcularMultaAtraso(ItemContratoPrazo itemContrato, long diasAtraso){
        
        if(diasAtraso <=0){
            return BigDecimal.ZERO;
        }
        
        BigDecimal percentualMulta = itemContrato.percentualMultaAtraso()
        .divide(BigDecimal.valueOf(100), MathContext.DECIMAL128);

        BigDecimal multa = BigDecimal.valueOf(diasAtraso)
        .multiply(itemContrato.valorDiaria())
        .multiply(percentualMulta)
        .min(itemContrato.valorLocacao());
        
        return multa.setScale(2,RoundingMode.HALF_UP);
    }

}
