package com.dcriar.api.hateoas.product.model;

import com.dcriar.domain.product.entity.enums.StatusAnaliseProduto;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@JsonRootName(value = "analiseEstoqueProduto")
@Relation(collectionRelation = "analises-estoque-produtos", itemRelation = "analise-estoque-produto")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnaliseEstoqueProdutoModel extends RepresentationModel<AnaliseEstoqueProdutoModel> {

    private Long produtoId;
    private String nomeProduto;
    private String skuProduto;
    private String tipoProduto;
    private Integer estoqueFisicoTotal;
    private Integer saldoConsiderado;
    private Integer estoqueDistribuidoTotal;
    private Integer estoqueDisponivelParaAlocar;
    private Integer estoqueCritico;

    @Schema(description = "Percentual de risco operacional com base no limite crítico.", example = "60.00")
    private BigDecimal percentualRisco;

    private StatusAnaliseProduto statusAnalise;
}
