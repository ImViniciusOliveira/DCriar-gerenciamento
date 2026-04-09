package com.dcriar.api.dto.response.stock;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AjusteLoteResumoDTO {

    @Schema(description = "ID do lote.", example = "3")
    private Long loteId;

    @Schema(description = "Identificador público do lote ou retalho.", example = "LT-000003")
    private String identificadorPublico;

    @Schema(description = "Identificador público do lote de origem, quando aplicável.", example = "LT-000001")
    private String identificadorOrigemPublico;

    @Schema(description = "ID do tipo de matéria-prima.", example = "4")
    private Long tipoMateriaPrimaId;

    @Schema(description = "Nome da matéria-prima.", example = "Adesivo BOPP Transparente")
    private String nomeTipoMateriaPrima;

    @Schema(description = "Tipo estrutural do item.", example = "LOTE_PRINCIPAL")
    private String tipoEstrutural;

    @Schema(description = "Saldo atual na unidade de apresentação.", example = "12.5000")
    private BigDecimal saldoEstoque;

    @Schema(description = "Símbolo da unidade de apresentação.", example = "m")
    private String unidadeSimbolo;

    @Schema(description = "Valor atual do lote.", example = "150.90")
    private BigDecimal valorAtualLote;

    @Schema(description = "Custo unitário atual na unidade de apresentação.", example = "12.07")
    private BigDecimal custoUnitarioAtual;
}
