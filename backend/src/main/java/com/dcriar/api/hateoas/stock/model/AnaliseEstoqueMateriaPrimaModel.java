package com.dcriar.api.hateoas.stock.model;

import com.dcriar.domain.stock.entity.enums.StatusAnaliseMateriaPrima;
import com.dcriar.domain.stock.entity.enums.UnidadeDeMedida;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@JsonRootName(value = "analiseEstoqueMateriaPrima")
@Relation(collectionRelation = "analises-estoque-materias-primas", itemRelation = "analise-estoque-materia-prima")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnaliseEstoqueMateriaPrimaModel extends RepresentationModel<AnaliseEstoqueMateriaPrimaModel> {

    private Long tipoMateriaPrimaId;
    private String nomeTipoMateriaPrima;
    private UnidadeDeMedida unidadeDeConsumo;
    private String unidadeDescricao;
    private String unidadeSimbolo;
    private String tipoProdutoCompativel;
    private BigDecimal saldoLotesPrincipais;
    private BigDecimal saldoRetalhos;
    private BigDecimal saldoTotal;
    private BigDecimal saldoConsiderado;
    private long quantidadeLotesPrincipais;
    private long quantidadeRetalhos;
    private BigDecimal estoqueCritico;
    @Schema(description = "Percentual de risco operacional com base no limite crítico.", example = "41.25")
    private BigDecimal percentualRisco;
    private StatusAnaliseMateriaPrima statusAnalise;
}
