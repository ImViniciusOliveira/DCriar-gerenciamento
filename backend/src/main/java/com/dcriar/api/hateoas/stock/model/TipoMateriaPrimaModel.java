package com.dcriar.api.hateoas.stock.model;

import com.dcriar.domain.stock.entity.enums.UnidadeDeMedida;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

/**
 * Modelo de representação HATEOAS para um Tipo de Matéria-Prima.
 * <p>
 * Representa um item de catálogo para um insumo, definindo suas características
 * gerais como nome e unidade de consumo padrão.
 */
@Getter
@Setter
@JsonRootName(value = "tipoMateriaPrima")
@Relation(collectionRelation = "tipos-materia-prima", itemRelation = "tipo-materia-prima")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TipoMateriaPrimaModel extends RepresentationModel<TipoMateriaPrimaModel> {

    @Schema(description = "ID único do tipo de matéria-prima.", example = "10")
    private Long id;

    @Schema(description = "Nome único do tipo de matéria-prima.", example = "Adesivo Vinil Branco Brilho")
    private String nome;

    @Schema(description = "Unidade de medida padrão para consumo em produção.", example = "CENTIMETRO_QUADRADO")
    private UnidadeDeMedida unidadeDeConsumo;

    @Schema(description = "Descrição amigável da unidade de medida.", example = "Metro Quadrado")
    private String unidadeDescricao;
}
