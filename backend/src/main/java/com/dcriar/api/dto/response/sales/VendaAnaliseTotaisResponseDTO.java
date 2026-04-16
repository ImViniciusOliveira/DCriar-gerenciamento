package com.dcriar.api.dto.response.sales;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendaAnaliseTotaisResponseDTO {

    @Schema(description = "Receita total do período.", example = "18420.90")
    private BigDecimal receita;

    @Schema(description = "Total de pedidos fechados no período.", example = "46")
    private Long totalPedidos;

    @Schema(description = "Receita do período anterior de mesma duração, imediatamente antes do período informado.", example = "16320.50")
    private BigDecimal receitaPeriodoAnterior;

    @Schema(description = "Variação percentual da receita em relação ao período anterior. Positivo indica crescimento, negativo indica queda. Nulo quando não há dados do período anterior.", example = "12.80")
    private BigDecimal deltaPercent;
}
