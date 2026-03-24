package com.dcriar.api.dto.request.production;

import com.dcriar.api.validation.annotation.ValidSimulacaoConsumoRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Data Transfer Object (DTO) para solicitar a simulação de consumo de matéria-prima.
 * <p>
 * Utilizado para calcular a quantidade total de matéria-prima necessária para produzir
 * uma certa quantidade de um produto de consumo (líquidos, pós, etc.),
 * sem efetivamente criar uma ordem de produção ou movimentar o estoque.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ValidSimulacaoConsumoRequest
public class SimulacaoConsumoRequestDTO {

    /**
     * O ID do produto para o qual a simulação será realizada.
     */
    @Schema(description = "ID do produto a ser simulado.", example = "13", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long produtoId;

    /**
     * A quantidade de unidades do produto que se deseja produzir.
     */
    @Schema(description = "Quantidade de unidades a serem produzidas na simulação.", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer quantidade;

    /**
     * O ID do lote de matéria-prima a ser considerado na simulação.
     */
    @Schema(description = "ID do lote de matéria-prima a ser considerado na simulação.", example = "11", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long loteId;

    @Schema(description = "ID opcional da ordem de produção em edição, para considerar o estorno no cálculo.", example = "9")
    private Long ordemId;
}
