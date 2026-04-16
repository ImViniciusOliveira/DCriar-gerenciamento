package com.dcriar.api.dto.response.product;

import com.dcriar.domain.product.entity.enums.StatusAnaliseProduto;
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
public class AnaliseEstoqueProdutoResponseDTO {

    @Schema(description = "ID do produto analisado.", example = "11")
    private Long produtoId;

    @Schema(description = "Nome do produto.", example = "Cartão de Visita Premium")
    private String nomeProduto;

    @Schema(description = "SKU do produto.", example = "CVP-300G")
    private String skuProduto;

    @Schema(description = "Tipo do produto.", example = "CONSUMO")
    private String tipoProduto;

    @Schema(description = "Saldo físico atual do produto.", example = "8")
    private Integer estoqueFisicoTotal;

    @Schema(description = "Saldo efetivamente considerado na análise do produto.", example = "8")
    private Integer saldoConsiderado;

    @Schema(description = "Saldo já distribuído pelos canais.", example = "5")
    private Integer estoqueDistribuidoTotal;

    @Schema(description = "Saldo ainda disponível para novas alocações.", example = "3")
    private Integer estoqueDisponivelParaAlocar;

    @Schema(description = "Quantidade mínima crítica configurada para o produto.", example = "20")
    private Integer estoqueCritico;

    @Schema(description = "Percentual de risco operacional com base no limite crítico. 0 significa saudável e 100 significa saldo zerado.", example = "60.00")
    private BigDecimal percentualRisco;

    @Schema(description = "Status operacional calculado para o produto.", example = "CRITICO")
    private StatusAnaliseProduto statusAnalise;
}
