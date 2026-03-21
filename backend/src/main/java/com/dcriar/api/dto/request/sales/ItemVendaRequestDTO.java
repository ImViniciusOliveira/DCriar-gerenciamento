package com.dcriar.api.dto.request.sales;

import com.dcriar.api.validation.annotation.ValidItemVendaRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

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
    @Schema(description = "O ID do produto que está sendo vendido.", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long produtoId;

    /**
     * A quantidade de unidades do produto vendidas.
     */
    @Schema(description = "A quantidade de unidades do produto vendidas.", example = "100", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer quantidade;

    @Schema(description = "Preço unitário aplicado ao item no momento da venda.", example = "19.90", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal precoAplicado;

    @Schema(description = "Preço total informado para o item.", example = "199.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal precoTotal;

    @Schema(description = "Como o preço foi definido no item.", example = "PRECO_PADRAO", requiredMode = Schema.RequiredMode.REQUIRED)
    private String tipoPrecoAplicado;

    @Schema(description = "Motivo da alteração quando o preço padrão não é utilizado.", example = "Desconto para fechamento")
    private String motivoAlteracaoPreco;
}
