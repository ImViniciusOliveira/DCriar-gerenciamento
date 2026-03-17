package com.dcriar.api.dto.request.production;

import com.dcriar.api.validation.annotation.ValidOrdemDeConsumoRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Data Transfer Object (DTO) para criar uma nova Ordem de Produção por Consumo.
 * <p>
 * Utilizado para produtos cuja matéria-prima não é medida geometricamente, como líquidos (litros),
 * pós (quilos) ou itens contáveis (unidades). A produção é registrada consumindo
 * uma quantidade específica de um lote de matéria-prima.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ValidOrdemDeConsumoRequest
public class OrdemDeConsumoRequestDTO {

    @Schema(description = "ID do produto a ser fabricado (deve ser um produto de consumo). No seed padrão, use 10 para 'Pacote de Ilhós Nº 0'.", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long produtoId;

    @Schema(description = "ID do lote de matéria-prima a ser consumido. No seed padrão, use 8 para 'Compra NF-1008'.", example = "8", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long loteId;

    @Schema(description = "ID do canal de venda de destino do estoque (opcional). No seed padrão, use 1 para 'Loja Física'.", example = "1")
    private Long canalVendaDestinoId;

    @Schema(description = "Quantidade de unidades do produto a serem produzidas. No seed padrão, 10 pacotes consomem exatamente 1000 unidades do lote 8.", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer quantidadeProduzida;

    @Schema(description = "Motivo ou referência para a ordem.", example = "Reposição de estoque de ilhós para Loja Física")
    private String motivo;
}
