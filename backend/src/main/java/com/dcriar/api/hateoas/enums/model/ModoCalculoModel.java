package com.dcriar.api.hateoas.enums.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

/**
 * Modelo de representação HATEOAS para o enum {@link com.dcriar.domain.production.enums.ModoCalculo}.
 * <p>
 * Expõe o nome do enum para ser consumido pelo frontend.
 */
@Relation(collectionRelation = "modosCalculo", itemRelation = "modoCalculo")
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class ModoCalculoModel extends RepresentationModel<ModoCalculoModel> {

    /**
     * O nome do valor do enum (ex: "AUTOMATICO").
     */
    private String name;
}
