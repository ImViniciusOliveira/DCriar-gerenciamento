package com.dcriar.api.dto.response.sales;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendaAnalisePorProdutoItemResponseDTO {

    @Schema(description = "ID do produto.", example = "11")
    private Long produtoId;

    @Schema(description = "Nome do produto.", example = "Cartão de Visita Premium")
    private String nomeProduto;

    @Schema(description = "SKU do produto.", example = "CVP-300G")
    private String skuProduto;

    @Schema(description = "Receita total gerada pelo produto no período.", example = "4860.00")
    private BigDecimal receita;

    @Schema(description = "Total de unidades vendidas do produto no período.", example = "18")
    private Long unidadesVendidas;
}
