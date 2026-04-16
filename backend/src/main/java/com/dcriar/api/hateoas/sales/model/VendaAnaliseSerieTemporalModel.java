package com.dcriar.api.hateoas.sales.model;

import com.dcriar.api.dto.response.sales.VendaAnaliseSerieItemResponseDTO;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

import java.util.List;

@Getter
@Setter
@Builder
@JsonRootName(value = "vendaAnaliseSeriesTemporal")
@Relation(collectionRelation = "vendas-analise-series-temporais", itemRelation = "venda-analise-serie-temporal")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VendaAnaliseSerieTemporalModel extends RepresentationModel<VendaAnaliseSerieTemporalModel> {

    private String trendLabel;
    private List<VendaAnaliseSerieItemResponseDTO> serie;
}
