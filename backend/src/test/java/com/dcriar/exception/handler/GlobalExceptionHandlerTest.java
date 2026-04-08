package com.dcriar.exception.handler;

import com.dcriar.api.dto.response.ErrorResponseDTO;
import com.dcriar.exception.custom.ArquivoStorageException;
import com.dcriar.exception.custom.DadosSensiveisCriptografiaException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldReturnControlledResponseForFileStorageException() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/produtos/1/foto");
        ArquivoStorageException exception = ArquivoStorageException.falhaAoArmazenar(
                "foto-produto.png",
                new RuntimeException("falha simulada no storage")
        );

        var response = handler.handleFileStorageException(exception, request);
        ErrorResponseDTO body = response.getBody();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(body);
        assertEquals(
                "Não foi possível armazenar o arquivo 'foto-produto.png'. Verifique o conteúdo enviado e tente novamente.",
                body.getMessage()
        );
        assertEquals("ARMAZENAR", body.getDetails().get("operacao"));
        assertEquals("foto-produto.png", body.getDetails().get("nomeArquivo"));
    }

    @Test
    void shouldReturnControlledResponseForSensitiveDataEncryptionException() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/vendas");
        DadosSensiveisCriptografiaException exception = DadosSensiveisCriptografiaException.chaveNaoConfigurada();

        var response = handler.handleSensitiveDataEncryption(exception, request);
        ErrorResponseDTO body = response.getBody();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(body);
        assertEquals(
                "A chave de criptografia dos dados sensíveis não foi configurada. Defina DATA_ENCRYPTION_KEY no ambiente.",
                body.getMessage()
        );
        assertEquals("CHAVE_CRIPTOGRAFIA_NAO_CONFIGURADA", body.getDetails().get("codigo"));
        assertEquals(
                "Revise a configuração de DATA_ENCRYPTION_KEY antes de iniciar a aplicação.",
                body.getDetails().get("orientacao")
        );
    }

    @Test
    void shouldReturnConflictResponseForGenericDataIntegrityViolation() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/recurso-teste");
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "violacao de integridade simulada",
                new RuntimeException("duplicate key value violates unique constraint")
        );

        var response = handler.handleDatabaseErrors(exception, request);
        ErrorResponseDTO body = response.getBody();

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(body);
        assertEquals(
                "A operação violou uma regra de integridade dos dados. Verifique se o registro já existe ou se ainda possui vínculos ativos.",
                body.getMessage()
        );
        assertEquals("VIOLACAO_DE_INTEGRIDADE", body.getDetails().get("causa"));
        assertEquals(
                "Verifique se o registro já existe ou se ainda está vinculado a outros dados.",
                body.getDetails().get("orientacao")
        );
    }
}
