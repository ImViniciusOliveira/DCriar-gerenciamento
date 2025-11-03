package com.dcriar.api.hateoas.enums.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

/**
 * Modelo de representação HATEOAS para o enum {@link com.dcriar.domain.product.entity.enums.TipoMovimentacaoProduto}.
 * <p>
 * Expõe o nome do enum para ser consumido pelo frontend.
 */
@Relation(collectionRelation = "tiposMovimentacaoProduto", itemRelation = "tipoMovimentacaoProduto")
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class TipoMovimentacaoProdutoModel extends RepresentationModel<TipoMovimentacaoProdutoModel> {

    /**
     * O nome do valor do enum (ex: "ENTRADA_PRODUCAO").
     */
    private String name;
}
