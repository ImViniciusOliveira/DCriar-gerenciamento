package com.dcriar.api.hateoas.enums.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

/**
 * Modelo de representação HATEOAS para o enum {@link com.dcriar.domain.stock.entity.enums.UnidadeDeMedida}.
 * <p>
 * Expõe os valores do enum para serem consumidos pelo frontend.
 */
@Relation(collectionRelation = "unidadesDeMedida", itemRelation = "unidadeDeMedida")
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class UnidadeDeMedidaModel extends RepresentationModel<UnidadeDeMedidaModel> {

    /**
     * O nome do valor do enum (ex: "METRO_LINEAR").
     */
    private String name;

    /**
     * A descrição textual da unidade de medida (ex: "Metro Linear").
     */
    private String descricao;

    /**
     * O símbolo da unidade de medida (ex: "m").
     */
    private String simbolo;
}
