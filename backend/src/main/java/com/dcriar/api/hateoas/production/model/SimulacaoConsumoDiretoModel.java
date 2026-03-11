package com.dcriar.api.hateoas.production.model;

import com.dcriar.api.dto.response.production.SimulacaoConsumoDiretoResponseDTO;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.Getter;
import lombok.Setter;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

@Relation(collectionRelation = "simulacoesConsumoDireto", itemRelation = "simulacaoConsumoDireto")
@Getter
@Setter
public class SimulacaoConsumoDiretoModel extends RepresentationModel<SimulacaoConsumoDiretoModel> {

    @JsonUnwrapped
    private SimulacaoConsumoDiretoResponseDTO data;
}
