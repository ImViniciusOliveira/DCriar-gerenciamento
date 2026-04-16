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
public class VendaAnaliseSerieItemResponseDTO {

    @Schema(description = "Rótulo principal do ponto na série (ex: '05/04', 'Abr').", example = "05/04")
    private String label;

    @Schema(description = "Rótulo auxiliar do ponto (ex: intervalo de datas do agrupamento). Nulo quando o label já é autoexplicativo.", example = "01/04 a 07/04")
    private String helperLabel;

    @Schema(description = "Receita total do segmento.", example = "2140.00")
    private BigDecimal receita;

    @Schema(description = "Total de pedidos fechados no segmento.", example = "6")
    private Long totalPedidos;
}
