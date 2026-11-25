package br.ufs.ziploc.disponibilidade.controller;

import br.ufs.ziploc.disponibilidade.api.DisponibilidadeDTO;
import br.ufs.ziploc.disponibilidade.service.DisponibilidadeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DisponibilidadeController.class)
class DisponibilidadeControllerTest {
    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private DisponibilidadeService disponibilidadeService;

    @Test
    void consultaPorPeriodoECategoria() throws Exception {
        when(disponibilidadeService.consultarDisponibilidade(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 10), "Andaime")).thenReturn(List.of(new DisponibilidadeDTO(10L, "Andaime tubular", "Andaime", new BigDecimal("50.00"), 3)));
        mvc.perform(get("/api/disponibilidade").param("inicio", "2026-12-01").param("fim", "2026-12-10").param("categoria", "Andaime").with(user("5").roles("ATENDENTE")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].modeloId").value(10)).andExpect(jsonPath("$[0].quantidadeDisponivel").value(3));
    }

    @Test
    void respondeRequisicaoInvalidaParaPeriodoInvertido() throws Exception {
        when(disponibilidadeService.consultarDisponibilidade(any(), any(), any())).thenThrow(new IllegalArgumentException("A data de fim deve ser posterior à data de início."));
        mvc.perform(get("/api/disponibilidade").param("inicio", "2026-12-10").param("fim", "2026-12-01").with(user("5").roles("ATENDENTE"))).andExpect(status().isBadRequest());
    }

    @Test
    void exigePeriodo() throws Exception {
        mvc.perform(get("/api/disponibilidade").with(user("5").roles("ATENDENTE"))).andExpect(status().isBadRequest());
    }

    @Test
    void exigeAutenticacao() throws Exception {
        mvc.perform(get("/api/disponibilidade").param("inicio", "2026-12-01").param("fim", "2026-12-10")).andExpect(status().isUnauthorized());
    }
}
