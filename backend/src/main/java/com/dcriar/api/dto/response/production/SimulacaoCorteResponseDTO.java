package com.dcriar.api.dto.response.production;

import com.dcriar.domain.production.enums.ModoCalculo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Data Transfer Object (DTO) para a resposta da simulação de uma produção por corte.
 * <p>
 * Este DTO informa as dimensões otimizadas do corte e o consumo de matéria-prima estimado
 * para produzir uma determinada quantidade de um produto, sem efetivamente criar uma ordem de produção.
 */
@Getter
@Setter
@Builder
public class SimulacaoCorteResponseDTO {

    @Schema(description = "Modo de cálculo sugerido ou utilizado na simulação.", example = "AUTOMATICO")
    private ModoCalculo modoCalculo;

    @Schema(description = "Largura final calculada para o corte (em cm).", example = "80.0")
    private BigDecimal larguraFinalCm;

    @Schema(description = "Comprimento final calculado para o corte (em cm).", example = "1200.0")
    private BigDecimal comprimentoFinalCm;

    @Schema(description = "Consumo estimado de matéria-prima (na unidade de estoque do lote, ex: metros lineares).", example = "12.0000")
    private BigDecimal consumoEstimado;

    @Schema(description = "Indica se o layout otimizado dos produtos foi rotacionado para melhor aproveitamento.", example = "true")
    private boolean rotacionado;

    @Schema(description = "Campo discriminador para identificar o tipo de resultado da simulação no frontend.", example = "CORTE", accessMode = Schema.AccessMode.READ_ONLY)
    private final String tipoSimulacao = "CORTE";
}
