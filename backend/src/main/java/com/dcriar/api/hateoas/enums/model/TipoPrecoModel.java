package com.dcriar.api.hateoas.enums.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

/**
 * Modelo de representação HATEOAS para o enum {@link com.dcriar.domain.product.entity.enums.TipoPreco}.
 * <p>
 * Expõe o nome do enum para ser consumido pelo frontend.
 */
@Relation(collectionRelation = "tiposPreco", itemRelation = "tipoPreco")
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class TipoPrecoModel extends RepresentationModel<TipoPrecoModel> {

    /**
     * O nome do valor do enum (ex: "VAREJO").
     */
    private String name;
}
