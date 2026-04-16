package com.dcriar.exception.handler;

import com.dcriar.exception.custom.OrdenacaoInvalidaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setConversionService(new DefaultFormattingConversionService())
                .build();
    }

    @Test
    void devePadronizarParametroObrigatorioAusente() throws Exception {
        mockMvc.perform(get("/test/totais")
                        .param("dataFim", "2026-04-16"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("O campo obrigatório 'data inicial' não foi informado na requisição."))
                .andExpect(jsonPath("$.details.parametro").value("dataInicio"))
                .andExpect(jsonPath("$.details.campo").value("data inicial"));
    }

    @Test
    void devePadronizarTipoInvalidoEmParametro() throws Exception {
        mockMvc.perform(get("/test/totais")
                        .param("dataInicio", "2026/04/10")
                        .param("dataFim", "2026-04-16"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("O campo 'data inicial' recebeu um valor inválido: '2026/04/10'."))
                .andExpect(jsonPath("$.details.parametro").value("dataInicio"))
                .andExpect(jsonPath("$.details.campo").value("data inicial"))
                .andExpect(jsonPath("$.details.valorInformado").value("2026/04/10"));
    }

    @Test
    void devePadronizarOrdenacaoInvalida() throws Exception {
        mockMvc.perform(get("/test/ordenacao-invalida"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("O campo de ordenação 'campoInexistente' não é suportado para 'por-canal'."))
                .andExpect(jsonPath("$.details.recurso").value("por-canal"))
                .andExpect(jsonPath("$.details.campoOrdenacao").value("campoInexistente"))
                .andExpect(jsonPath("$.details.camposAceitos").value("receita, totalPedidos, nomeCanal, canalVendaId"));
    }

    @Test
    void devePadronizarJsonMalformado() throws Exception {
        mockMvc.perform(post("/test/json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantidade\": }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("O corpo da requisição está malformado ou contém dados inválidos."))
                .andExpect(jsonPath("$.details.causa").exists());
    }

    @RestController
    @RequestMapping("/test")
    static class TestController {

        @GetMapping("/totais")
        String totais(
                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim
        ) {
            return dataInicio + ":" + dataFim;
        }

        @GetMapping("/ordenacao-invalida")
        String ordenacaoInvalida() {
            throw new OrdenacaoInvalidaException(
                    "por-canal",
                    "campoInexistente",
                    "receita, totalPedidos, nomeCanal, canalVendaId"
            );
        }

        @PostMapping("/json")
        Map<String, Object> json(@RequestBody Map<String, Object> payload) {
            return payload;
        }
    }
}
