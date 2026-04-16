package com.dcriar.api.hateoas.sales.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@JsonRootName(value = "vendaAnalisePorProduto")
@Relation(collectionRelation = "vendas-analise-por-produto", itemRelation = "venda-analise-por-produto")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VendaAnalisePorProdutoModel extends RepresentationModel<VendaAnalisePorProdutoModel> {

    private Long produtoId;
    private String nomeProduto;
    private String skuProduto;
    private BigDecimal receita;
    private Long unidadesVendidas;
}
