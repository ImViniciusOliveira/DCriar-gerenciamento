package com.dcriar.api.dto.response.production;

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
public class PlanoDeConsumoItemDTO {

    @Schema(description = "ID do lote que será consumido.")
    private Long loteId;

    @Schema(description = "Motivo ou identificador do lote (ex: número da nota fiscal).")
    private String motivoLote;

    @Schema(description = "Quantidade exata que será consumida deste lote.")
    private BigDecimal quantidadeAConsumir;
}
