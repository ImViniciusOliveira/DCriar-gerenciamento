package com.dcriar.api.hateoas.enums.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

/**
 * Modelo de representação HATEOAS para o enum {@link com.dcriar.domain.stock.entity.enums.TipoMovimentacao}.
 * <p>
 * Expõe o nome do enum para ser consumido pelo frontend.
 */
@Relation(collectionRelation = "tiposMovimentacao", itemRelation = "tipoMovimentacao")
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class TipoMovimentacaoModel extends RepresentationModel<TipoMovimentacaoModel> {

    /**
     * O nome do valor do enum (ex: "ENTRADA_COMPRA").
     */
    private String name;
}
