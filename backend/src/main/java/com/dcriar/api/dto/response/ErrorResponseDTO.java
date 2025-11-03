package com.dcriar.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;
import java.util.Map;

/**
 * Data Transfer Object (DTO) padronizado para respostas de erro da API.
 * <p>
 * Este DTO fornece uma estrutura consistente para comunicar erros aos clientes da API,
 * incluindo um timestamp, o status HTTP, uma mensagem de erro e detalhes adicionais,
 * como erros de validação de campos.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponseDTO {

    /**
     * O momento em que o erro ocorreu, em UTC.
     */
    @Schema(description = "O momento em que o erro ocorreu, em UTC.", example = "2023-10-27T10:00:00Z")
    private Instant timestamp;

    /**
     * O código de status HTTP.
     */
    @Schema(description = "O código de status HTTP.", example = "400")
    private int status;

    /**
     * A descrição textual do status HTTP (ex: "Bad Request").
     */
    @Schema(description = "A descrição textual do status HTTP.", example = "Bad Request")
    private String error;

    /**
     * Uma mensagem clara e concisa descrevendo o erro.
     */
    @Schema(description = "Uma mensagem clara e concisa descrevendo o erro.", example = "Ocorreu um erro de validação.")
    private String message;

    /**
     * Um mapa opcional contendo detalhes adicionais sobre o erro.
     * <p>
     * Útil para retornar múltiplos erros de validação de uma vez, onde a chave é o campo
     * e o valor é a mensagem de erro.
     */
    @Schema(description = "Detalhes adicionais sobre o erro, como erros de validação de campos.", example = "{\"fieldName\": \"A mensagem de erro.\"}")
    private Map<String, String> details;
}
