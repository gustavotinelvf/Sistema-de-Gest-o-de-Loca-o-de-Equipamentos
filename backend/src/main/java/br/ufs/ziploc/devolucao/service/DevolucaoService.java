package br.ufs.ziploc.devolucao.service;

import br.ufs.ziploc.devolucao.domain.Devolucao;
import br.ufs.ziploc.devolucao.dto.DevolucaoDto;
import br.ufs.ziploc.devolucao.dto.RegistrarDevolucaoDto;
import br.ufs.ziploc.devolucao.exceptions.ItemContratoNaoEncontradoException;
import br.ufs.ziploc.devolucao.repository.DevolucaoRepository;
import br.ufs.ziploc.faturamento.api.FaturaApi;
import br.ufs.ziploc.locacao.api.ContratoApi;
import br.ufs.ziploc.locacao.api.ContratoResumo;
import br.ufs.ziploc.locacao.api.ItemContratoPrazo;
import br.ufs.ziploc.shared.exception.ConflitoException;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DevolucaoService {

    private static final ZoneId FUSO_LOCAL =
            ZoneId.of("America/Sao_Paulo");

    private static final long DIAS_CARENCIA = 7;

    private static final String DEVOLUCAO_DUPLICADA =
            "Este item de contrato já foi devolvido.";

    private final DevolucaoRepository devolucaoRepository;
    private final ContratoApi contratoApi;
    private final FaturaApi faturaApi;

    @Transactional
    public DevolucaoDto registrar(
            RegistrarDevolucaoDto request,
            Long locadoraId,
            Long usuarioId) {

        validarIdentificador(locadoraId, "Locadora");
        validarIdentificador(usuarioId, "Usuário");

        if (request == null) {
            throw new IllegalArgumentException(
                    "Os dados da devolução são obrigatórios.");
        }

        validarIdentificador(request.itemContratoId(), "Item de contrato");

        String observacoes = normalizarObservacoes(request.observacoes());

        // Bloqueia o contrato antes de consultar multas ou devoluções.
        ContratoResumo contrato = contratoApi
                .buscarContratoParaDevolucao(
                        request.itemContratoId(), locadoraId)
                .orElseThrow(() -> new ItemContratoNaoEncontradoException(
                        request.itemContratoId()));

        ItemContratoPrazo itemContrato = contrato.itens().stream()
                .filter(item -> item.id().equals(request.itemContratoId()))
                .findFirst()
                .orElseThrow(() -> new ItemContratoNaoEncontradoException(
                        request.itemContratoId()));

        // A consulta ao repositório garante a verificação atualizada.
        if (devolucaoRepository
                .findByItemContratoId(request.itemContratoId())
                .isPresent()) {

            throw new ConflitoException(DEVOLUCAO_DUPLICADA);
        }

        if (!"ATIVO".equals(contrato.status())) {
            throw new IllegalStateException(
                    "A devolução exige contrato ATIVO. Status atual: "
                            + contrato.status());
        }

        if (itemContrato.dataInicioReal() == null) {
            throw new IllegalStateException(
                    "O item não possui retirada registrada.");
        }

        Instant dataDevolucao = Instant.now();

        LocalDate diaDevolucao = dataDevolucao
                .atZone(FUSO_LOCAL)
                .toLocalDate();

        if (diaDevolucao.isBefore(itemContrato.dataInicioReal())) {
            throw new IllegalStateException(
                    "A devolução não pode ser anterior à retirada.");
        }

        BigDecimal multaCalculada =
                calcularMultaAtraso(itemContrato, diaDevolucao);

        BigDecimal multasAnteriores =
                somarMultasRegistradas(contrato);

        BigDecimal saldoDoTeto = contrato.valorTotal()
                .subtract(multasAnteriores)
                .max(BigDecimal.ZERO);

        BigDecimal multaLimitada = multaCalculada
                .min(saldoDoTeto)
                .setScale(2, RoundingMode.HALF_UP);

        Devolucao devolucao = Devolucao.builder()
                .locadoraId(locadoraId)
                .itemContratoId(request.itemContratoId())
                .usuarioId(usuarioId)
                .dataDevolucaoReal(dataDevolucao)
                .multaAtraso(multaLimitada)
                .observacoes(observacoes)
                .build();

        try {
            devolucao = devolucaoRepository.saveAndFlush(devolucao);

        } catch (DataIntegrityViolationException e) {
            if (violouUnicidadeDaDevolucao(e)) {
                throw new ConflitoException(DEVOLUCAO_DUPLICADA);
            }

            throw e;
        }

        // A fatura só é gerada quando todas as unidades forem devolvidas.
        if (contratoApi.contarItensPendentes(contrato.id()) == 0) {

            BigDecimal somaDasMultas =
                    multasAnteriores.add(multaLimitada);

            faturaApi.gerarFatura(
                    contrato.id(),
                    locadoraId,
                    usuarioId,
                    somaDasMultas);
        }

        return DevolucaoDto.recebimentoDevolucao(devolucao);
    }

    /**
     * RD015: sete dias de carência.
     * A cobrança começa no oitavo dia, sem retroatividade.
     */
    static BigDecimal calcularMultaAtraso(
            ItemContratoPrazo itemContrato,
            LocalDate diaDevolucao) {

        long diasDeAtraso = Math.max(
                0,
                ChronoUnit.DAYS.between(
                        itemContrato.dataFimPrevista(),
                        diaDevolucao));

        long diasCobrados = Math.max(
                0, diasDeAtraso - DIAS_CARENCIA);

        BigDecimal percentualMulta = itemContrato
                .percentualMulta()
                .movePointLeft(2);

        return BigDecimal.valueOf(diasCobrados)
                .multiply(itemContrato.valorDiariaContratada())
                .multiply(percentualMulta)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal somarMultasRegistradas(
            ContratoResumo contrato) {

        List<Long> ids = contrato.itens().stream()
                .map(ItemContratoPrazo::id)
                .toList();

        return devolucaoRepository.findByItemContratoIdIn(ids)
                .stream()
                .map(Devolucao::getMultaAtraso)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static void validarIdentificador(
            Long id, String campo) {

        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    campo + " possui identificador inválido.");
        }
    }

    private static String normalizarObservacoes(
            String observacoes) {

        if (observacoes == null || observacoes.isBlank()) {
            return null;
        }

        String normalizadas = observacoes.strip();

        if (normalizadas.length() > 500) {
            throw new IllegalArgumentException(
                    "As observações devem possuir no máximo 500 caracteres.");
        }

        return normalizadas;
    }

    private static boolean violouUnicidadeDaDevolucao(
            Throwable erro) {

        Throwable causa = erro;

        while (causa != null) {
            if (causa instanceof ConstraintViolationException violacao
                    && "23505".equals(violacao.getSQLState())
                    && "devolucao_item_contrato_id_key"
                            .equals(violacao.getConstraintName())) {

                return true;
            }

            causa = causa.getCause();
        }

        return false;
    }
}