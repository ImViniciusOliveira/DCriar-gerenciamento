package com.dcriar.api.hateoas.stock.model;

import com.dcriar.domain.stock.entity.enums.TipoMovimentacao;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Modelo de representação HATEOAS para uma Movimentação de Lote de Matéria-Prima.
 * <p>
 * Representa um único evento no histórico de um lote, como uma entrada por compra
 * ou uma saída para produção.
 */
@Getter
@Setter
@JsonRootName(value = "movimentacaoLote")
@Relation(collectionRelation = "movimentacoes", itemRelation = "movimentacao")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MovimentacaoLoteModel extends RepresentationModel<MovimentacaoLoteModel> {

    @Schema(description = "ID único da movimentação.", example = "50")
    private Long id;

    @Schema(description = "Data e hora em que a movimentação foi registrada.")
    private OffsetDateTime data;

    @Schema(description = "Tipo da movimentação (ex: ENTRADA_COMPRA, SAIDA_PRODUCAO).", example = "SAIDA_PRODUCAO")
    private TipoMovimentacao tipo;

    @Schema(description = "Quantidade movimentada. Positiva para entradas, negativa para saídas.", example = "-5.5000")
    private BigDecimal quantidade;

    @Schema(description = "Custo por unidade base, preenchido em entradas por compra.", nullable = true, example = "0.01250000")
    private BigDecimal custoPorUnidadeBase;

    @Schema(description = "Motivo ou referência para a movimentação.", example = "Consumido pela Ordem de Produção #3")
    private String motivo;
}
