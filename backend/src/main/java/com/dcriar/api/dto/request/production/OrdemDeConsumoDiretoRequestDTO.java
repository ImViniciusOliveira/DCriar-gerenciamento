package com.dcriar.api.dto.request.production;

import com.dcriar.api.validation.annotation.ValidOrdemDeConsumoDiretoRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Data Transfer Object (DTO) para criar uma nova Ordem de Produção por Consumo Direto.
 * <p>
 * Utilizado para produtos cuja matéria-prima não é medida geometricamente, como líquidos (litros),
 * pós (quilos) ou itens contáveis (unidades). A produção é registrada consumindo
 * uma quantidade específica de um ou mais lotes de matéria-prima.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ValidOrdemDeConsumoDiretoRequest
public class OrdemDeConsumoDiretoRequestDTO {

    @Schema(description = "ID do produto a ser fabricado (deve ser um produto de consumo direto, como líquidos, pós ou unidades).", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long produtoId;

    @Schema(description = "Lista de IDs dos lotes de matéria-prima a serem consumidos.", example = "[4]", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> lotesConsumidosIds;

    @Schema(description = "ID do canal de venda de destino do estoque (opcional). Se fornecido, o estoque produzido será alocado neste canal.", example = "1")
    private Long canalVendaDestinoId;

    @Schema(description = "Quantidade de unidades do produto a serem produzidas.", example = "250", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer quantidadeProduzida;

    @Schema(description = "Motivo ou referência para a ordem.", example = "Reposição de Estoque Interno")
    private String motivo;
}
