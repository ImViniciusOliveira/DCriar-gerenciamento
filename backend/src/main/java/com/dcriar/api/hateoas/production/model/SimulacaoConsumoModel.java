package com.dcriar.api.hateoas.production.model;

import com.dcriar.api.dto.response.production.SimulacaoConsumoResponseDTO;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.Getter;
import lombok.Setter;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

@Relation(collectionRelation = "simulacoesConsumo", itemRelation = "simulacaoConsumo")
@Getter
@Setter
public class SimulacaoConsumoModel extends RepresentationModel<SimulacaoConsumoModel> {

    @JsonUnwrapped
    private SimulacaoConsumoResponseDTO data;
}
