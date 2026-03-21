package com.dcriar.api.dto.response.sales;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

/**
 * Data Transfer Object (DTO) que representa um item de linha em uma Venda.
 * <p>
 * Cada objeto deste DTO corresponde a um produto e a sua respectiva quantidade,
 * preço e valor total dentro da lista de itens de uma {@link VendaResponseDTO}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemVendaResponseDTO {

    @Schema(description = "O ID único do item da venda.", example = "1")
    private Long id;

    @Schema(description = "O ID do produto vendido.", example = "101")
    private Long produtoId;

    @Schema(description = "O SKU (Stock Keeping Unit) do produto vendido.", example = "ETQ-KFT-RD-50")
    private String produtoSku;

    @Schema(description = "O nome descritivo do produto vendido.", example = "Etiqueta Redonda Kraft 5x5cm")
    private String nomeProduto;

    @Schema(description = "A quantidade de unidades do produto vendidas.", example = "2")
    private Integer quantidade;

    @Schema(description = "Preço comercial padrão do produto no momento da venda.", example = "25.00")
    private BigDecimal precoComercialOriginal;

    @Schema(description = "O preço unitário do produto no momento da venda.", example = "25.00")
    private BigDecimal precoUnitario;

    @Schema(description = "O preço total para este item (quantidade * preço unitário).", example = "50.00")
    private BigDecimal precoTotal;

    @Schema(description = "Como o preço do item foi definido.", example = "PRECO_PADRAO")
    private String tipoPrecoAplicado;

    @Schema(description = "Motivo registrado quando o preço padrão não foi utilizado.", example = "Cliente recorrente")
    private String motivoAlteracaoPreco;
}
