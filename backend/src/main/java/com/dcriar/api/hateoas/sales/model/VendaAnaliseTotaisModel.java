package com.dcriar.api.hateoas.sales.model;

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
@JsonRootName(value = "vendaAnaliseTotais")
@Relation(collectionRelation = "vendas-analise-totais", itemRelation = "venda-analise-totais")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VendaAnaliseTotaisModel extends RepresentationModel<VendaAnaliseTotaisModel> {

    private BigDecimal receita;
    private Long totalPedidos;
    private BigDecimal receitaPeriodoAnterior;

    @Schema(description = "Variação percentual da receita em relação ao período anterior. Nulo quando não há dados do período anterior.")
    private BigDecimal deltaPercent;
}
