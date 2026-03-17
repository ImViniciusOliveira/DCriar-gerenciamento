package com.dcriar.api.hateoas.production.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@JsonRootName(value = "ordemDeConsumo")
@Relation(collectionRelation = "ordensDeConsumo", itemRelation = "ordemDeConsumo")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrdemDeConsumoModel extends RepresentationModel<OrdemDeConsumoModel> {

    @Schema(description = "ID único da ordem de produção.")
    private Long id;

    @Schema(description = "ID do produto final fabricado.")
    private Long produtoId;

    @Schema(description = "Nome do produto final fabricado.")
    private String nomeProduto;

    @Schema(description = "Lista de IDs dos lotes de matéria-prima consumidos.")
    private List<Long> lotesConsumidosIds;

    @Schema(description = "Quantidade de unidades do produto que foram produzidas.")
    private Integer quantidadeProduzida;

    @Schema(description = "Data e hora em que a ordem foi criada.")
    private LocalDateTime dataCriacao;

    @Schema(description = "Data e hora da última atualização da ordem.")
    private LocalDateTime dataAtualizacao;

    @Schema(description = "Motivo ou referência para a ordem.")
    private String motivo;
}
