package com.dcriar.api.dto.request.product;

import com.dcriar.api.validation.annotation.ValidMovimentacaoEstoqueProdutoRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * DTO para requisições de criação/atualização de movimentação de estoque de produto acabado.
 * <p>
 * Centraliza os dados necessários para registrar uma movimentação no livro-razão do estoque.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ValidMovimentacaoEstoqueProdutoRequest
public class MovimentacaoEstoqueProdutoRequestDTO {

    /**
     * ID do produto associado à movimentação.
     */
    @Schema(description = "ID do produto associado à movimentação.", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long produtoId;

    /**
     * Tipo da movimentação (ex: ENTRADA_PRODUCAO, SAIDA_VENDA).
     */
    @Schema(description = "Tipo da movimentação (ex: ENTRADA_PRODUCAO, SAIDA_VENDA).", example = "ENTRADA_PRODUCAO", requiredMode = Schema.RequiredMode.REQUIRED)
    private String tipo;

    /**
     * Quantidade movimentada. Positiva para entradas, negativa para saídas.
     */
    @Schema(description = "Quantidade movimentada. Positiva para entradas, negativa para saídas.", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer quantidade;

    /**
     * Motivo ou observação da movimentação.
     */
    @Schema(description = "Motivo ou observação da movimentação.", example = "Ajuste manual")
    private String motivo;
}
