package com.dcriar.api.hateoas.enums.model;

import com.dcriar.api.dto.response.product.CanalVendaResponseDTO;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

/**
 * Modelo HATEOAS para a resposta de Canal de Venda.
 */
@Getter
@AllArgsConstructor
@Relation(collectionRelation = "canais-venda", itemRelation = "canal-venda")
public class CanalVendaModel extends RepresentationModel<CanalVendaModel> {

    @JsonUnwrapped
    private final CanalVendaResponseDTO dto;
}
