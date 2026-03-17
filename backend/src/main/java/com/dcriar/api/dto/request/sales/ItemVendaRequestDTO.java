package com.dcriar.api.dto.request.sales;

import com.dcriar.api.validation.annotation.ValidItemVendaRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * Data Transfer Object (DTO) para receber os dados de um item de linha em uma Venda.
 * <p>
 * Cada objeto deste DTO corresponde a um produto e sua respectiva quantidade
 * dentro da lista de itens de uma {@link VendaRequestDTO}. A validação dos campos
 * é garantida pela anotação {@link ValidItemVendaRequest}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ValidItemVendaRequest
public class ItemVendaRequestDTO {

    /**
     * O ID do produto que está sendo vendido.
     */
    @Schema(description = "O ID do produto que está sendo vendido. No seed padrão, use 11 para 'Kit de Resina Epóxi'.", example = "11", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long produtoId;

    /**
     * A quantidade de unidades do produto vendidas.
     */
    @Schema(description = "A quantidade de unidades do produto vendidas. No seed padrão, 1 unidade do produto 11 é um caso feliz na Loja Física.", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer quantidade;
}
