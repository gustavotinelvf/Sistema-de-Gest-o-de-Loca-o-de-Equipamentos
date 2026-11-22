package br.ufs.ziploc.locacao.controller;

import br.ufs.ziploc.locacao.dto.SolicitacaoDTO;
import br.ufs.ziploc.locacao.service.SolicitacaoNaoEncontradaException;
import br.ufs.ziploc.locacao.service.SolicitacaoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SolicitacaoController.class)
class SolicitacaoControllerTest {
    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private SolicitacaoService solicitacaoService;

    private MockHttpServletRequestBuilder reprovar(Long id, String corpo) {
        return post("/api/solicitacoes/{id}/reprovacao", id).with(csrf()).with(user("5").roles("GERENTE")).contentType(MediaType.APPLICATION_JSON).content(corpo);
    }

    @Test
    void reprovaComJustificativa() throws Exception {
        SolicitacaoDTO dto = new SolicitacaoDTO(7L, 1L, 2L, 5L, null, null, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 10), "REJEITADA", "Motivo", List.of());
        when(solicitacaoService.reprovar(7L, 5L, "Motivo")).thenReturn(dto);
        mvc.perform(reprovar(7L, "{\"justificativa\":\"Motivo\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REJEITADA")).andExpect(jsonPath("$.analisadoPorId").value(5));
    }

    @Test
    void recusaJustificativaEmBranco() throws Exception {
        mvc.perform(reprovar(7L, "{\"justificativa\":\" \"}")).andExpect(status().isBadRequest());
        verifyNoInteractions(solicitacaoService);
    }

    @Test
    void respondeNaoEncontrado() throws Exception {
        when(solicitacaoService.reprovar(eq(99L), any(), anyString())).thenThrow(new SolicitacaoNaoEncontradaException(99L));
        mvc.perform(reprovar(99L, "{\"justificativa\":\"Motivo\"}")).andExpect(status().isNotFound());
    }

    @Test
    void respondeConflitoQuandoJaAnalisada() throws Exception {
        when(solicitacaoService.reprovar(eq(7L), any(), anyString())).thenThrow(new IllegalStateException("Já analisada."));
        mvc.perform(reprovar(7L, "{\"justificativa\":\"Motivo\"}")).andExpect(status().isConflict());
    }

    @Test
    void exigeAutenticacao() throws Exception {
        mvc.perform(post("/api/solicitacoes/7/reprovacao").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"justificativa\":\"Motivo\"}")).andExpect(status().isUnauthorized());
    }
}
