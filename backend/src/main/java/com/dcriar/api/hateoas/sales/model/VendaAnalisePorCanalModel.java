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
@JsonRootName(value = "vendaAnalisePorCanal")
@Relation(collectionRelation = "vendas-analise-por-canal", itemRelation = "venda-analise-por-canal")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VendaAnalisePorCanalModel extends RepresentationModel<VendaAnalisePorCanalModel> {

    private Long canalVendaId;
    private String nomeCanal;
    private BigDecimal receita;
    private Long totalPedidos;
}
