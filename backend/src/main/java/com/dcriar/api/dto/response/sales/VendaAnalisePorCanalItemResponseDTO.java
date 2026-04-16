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
public class VendaAnalisePorCanalItemResponseDTO {

    @Schema(description = "ID do canal de venda.", example = "3")
    private Long canalVendaId;

    @Schema(description = "Nome do canal de venda.", example = "Shopee")
    private String nomeCanal;

    @Schema(description = "Receita total gerada pelo canal no período.", example = "8240.50")
    private BigDecimal receita;

    @Schema(description = "Total de pedidos fechados pelo canal no período.", example = "17")
    private Long totalPedidos;
}
