package com.dcriar.api.hateoas.enums.model;

import java.math.BigDecimal;

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
     * A descrição textual plural da unidade de medida (ex: "Metros Lineares").
     */
    private String descricaoPlural;

    /**
     * O símbolo da unidade de medida (ex: "m").
     */
    private String simbolo;

    /**
     * Indica se a quantidade deve ser exibida com o símbolo da unidade, sem espaço.
     */
    private boolean exibirQuantidadeComSimbolo;

    /**
     * Unidade menor compatível para informar quantidades no cadastro de consumo.
     */
    private String unidadeCadastroCompativel;

    /**
     * Fator de conversão da unidade compatível para a unidade principal.
     */
    private BigDecimal fatorConversaoCadastroCompativel;
}
