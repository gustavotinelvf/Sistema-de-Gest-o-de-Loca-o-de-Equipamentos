package br.ufs.ziploc.acesso.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import br.ufs.ziploc.acesso.api.ParametrosLocadoraDTO;
import br.ufs.ziploc.acesso.domain.EnderecoLocadora;
import br.ufs.ziploc.acesso.domain.Locadora;
import br.ufs.ziploc.acesso.repository.LocadoraRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LocadoraServiceTest {

    @Mock
    private LocadoraRepository locadoraRepository;

    @InjectMocks
    private LocadoraService locadoraService;

    @Test
    void obterParametros_devolveOsDadosDaLocadora() {
        var endereco = new EnderecoLocadora("49000000", "Rua A", "10", null,
                "Centro", "Aracaju", "SE");
        var locadora = new Locadora("ZipLoc Ltda", "12345678000199", endereco,
                new BigDecimal("30.00"), new BigDecimal("2.50"));
        when(locadoraRepository.findUnica()).thenReturn(Optional.of(locadora));

        ParametrosLocadoraDTO dto = locadoraService.obterParametros();

        assertEquals("ZipLoc Ltda", dto.razaoSocial());
        assertEquals("12345678000199", dto.cnpj());
        assertEquals("49000000", dto.cepSede());
        assertEquals("SE", dto.ufSede());
        assertNull(dto.complementoSede());
        assertEquals(new BigDecimal("30.00"), dto.raioEntregaKm());
        assertEquals(new BigDecimal("2.50"), dto.percentualMultaPadrao());
    }

    @Test
    void obterParametros_semLocadora_lancaIllegalState() {
        when(locadoraRepository.findUnica()).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> locadoraService.obterParametros());
    }
}
