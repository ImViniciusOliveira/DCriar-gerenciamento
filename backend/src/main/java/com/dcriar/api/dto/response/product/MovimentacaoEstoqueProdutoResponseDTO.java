package com.dcriar.api.dto.response.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * DTO para resposta de movimentação de estoque de produto acabado.
 * <p>
 * Expõe os dados registrados no livro-razão do estoque.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovimentacaoEstoqueProdutoResponseDTO {

    /**
     * ID da movimentação.
     */
    @Schema(description = "ID da movimentação.", example = "1")
    private Long id;

    /**
     * ID do produto associado.
     */
    @Schema(description = "ID do produto.", example = "1")
    private Long produtoId;

    /**
     * Data/hora da movimentação.
     */
    @Schema(description = "Data/hora da movimentação.", example = "2025-10-13T14:00:00Z")
    private OffsetDateTime data;

    /**
     * Tipo da movimentação.
     */
    @Schema(description = "Tipo da movimentação.", example = "ENTRADA_PRODUCAO")
    private String tipo;

    /**
     * Quantidade movimentada.
     */
    @Schema(description = "Quantidade movimentada.", example = "10")
    private Integer quantidade;

    /**
     * Motivo ou observação.
     */
    @Schema(description = "Motivo ou observação.", example = "Ajuste manual")
    private String motivo;
}

