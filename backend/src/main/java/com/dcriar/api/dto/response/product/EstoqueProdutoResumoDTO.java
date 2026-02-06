package com.dcriar.api.dto.response.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

/**
 * DTO de resumo para listagem de estoque de produtos em um canal específico.
 * Otimizado para autocompletes e buscas rápidas, contendo apenas os dados essenciais
 * do produto e sua disponibilidade no canal consultado.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstoqueProdutoResumoDTO {

    @Schema(description = "ID do produto.", example = "1")
    private Long produtoId;

    @Schema(description = "Nome do produto.", example = "Cartão de Visita")
    private String nomeProduto;

    @Schema(description = "SKU do produto.", example = "CV-001")
    private String skuProduto;

    @Schema(description = "Quantidade disponível no canal consultado.", example = "50")
    private Integer quantidadeNoCanal;

    @Schema(description = "Preço de venda sugerido (varejo).", example = "99.90")
    private BigDecimal precoVenda;
}
