package com.dcriar.api.dto.request.production;

import com.dcriar.api.validation.annotation.ValidSimulacaoCorteRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Data Transfer Object (DTO) para solicitar a simulação de uma produção por corte.
 * <p>
 * Utilizado para estimar o consumo de matéria-prima para um produto de corte geométrico.
 * O sistema simula o melhor layout de corte (considerando a rotação da peça) para
 * calcular o consumo de material sem efetivamente criar uma ordem de produção.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ValidSimulacaoCorteRequest
public class SimulacaoCorteRequestDTO {

    /**
     * O ID do produto para o qual a simulação de corte será realizada.
     */
    @Schema(description = "ID do produto a ser simulado. No seed padrão, use 3 para 'Adesivo Redondo 5cm'.", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long produtoId;

    /**
     * A quantidade de unidades do produto que se deseja produzir.
     */
    @Schema(description = "Quantidade de unidades a serem produzidas na simulação. No seed padrão, 10 unidades do produto 3 cabem confortavelmente no lote 3.", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer quantidade;
    /**
     * O ID do lote de matéria-prima a ser utilizado na simulação.
     */
    @Schema(description = "ID do lote de matéria-prima a ser utilizado na simulação. No seed padrão, use 3 para 'Compra NF-1003'.", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long loteId;
}
