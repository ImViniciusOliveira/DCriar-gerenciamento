package com.dcriar.api.hateoas.production.model;

import com.dcriar.api.dto.response.production.SimulacaoCorteResponseDTO;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.Getter;
import lombok.Setter;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

@Relation(collectionRelation = "simulacoesCorte", itemRelation = "simulacaoCorte")
@Getter
@Setter
public class SimulacaoCorteModel extends RepresentationModel<SimulacaoCorteModel> {

    @JsonUnwrapped
    private SimulacaoCorteResponseDTO data;
}
