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
    @Schema(description = "ID do produto associado ao estoque.", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long produtoId;

    /**
     * ID do canal de venda onde o estoque está alocado.
     */
    @Schema(description = "ID do canal de venda onde o estoque está alocado.", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long canalVendaId;

    /**
     * Quantidade de unidades disponíveis.
     */
    @Schema(description = "Quantidade de unidades disponíveis.", example = "1000", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer quantidade;
}
