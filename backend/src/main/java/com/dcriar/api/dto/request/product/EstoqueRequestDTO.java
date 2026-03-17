package com.dcriar.api.dto.request.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * DTO para requisições de criação/atualização de estoque de produto em canal de venda.
 * <p>
 * Centraliza os dados necessários para manipulação de estoque.
 * <p>
 * Este DTO não usa validador personalizado próprio porque é usado internamente entre service e entidade,
 * e não como payload HTTP validado com {@code @Valid}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstoqueRequestDTO {

    /**
     * ID do produto associado ao estoque.
     */
    @Schema(description = "ID do produto associado ao estoque. No seed padrão, use 11 para 'Kit de Resina Epóxi'.", example = "11", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long produtoId;

    /**
     * ID do canal de venda onde o estoque está alocado.
     */
    @Schema(description = "ID do canal de venda onde o estoque está alocado. No seed padrão, use 1 para 'Loja Física'.", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long canalVendaId;

    /**
     * Quantidade de unidades disponíveis.
     */
    @Schema(description = "Quantidade de unidades disponíveis. No seed padrão, o produto 11 está com 8 unidades no canal 1.", example = "8", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer quantidade;
}
