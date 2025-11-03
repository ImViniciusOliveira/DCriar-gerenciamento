package com.dcriar.api.hateoas.sales.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Modelo de representação HATEOAS para uma Venda.
 * <p>
 * Este modelo expõe os dados de uma venda e inclui links para recursos relacionados,
 * seguindo os princípios do HATEOAS.
 */
@Getter
@Setter
@JsonRootName(value = "venda")
@Relation(collectionRelation = "vendas", itemRelation = "venda")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VendaModel extends RepresentationModel<VendaModel> {

    @Schema(description = "ID único da venda.", example = "1")
    private Long id;

    @Schema(description = "Data e hora em que a venda foi registrada.")
    private OffsetDateTime dataVenda;

    @Schema(description = "Nome do canal onde a venda ocorreu.", example = "LOJA_FISICA")
    private String nomeCanalVenda;

    @Schema(description = "Valor total da venda.", example = "125.50")
    private BigDecimal valorTotal;

    @Schema(description = "Lista de itens que compõem a venda.")
    private List<ItemVendaModel> itens;
}
