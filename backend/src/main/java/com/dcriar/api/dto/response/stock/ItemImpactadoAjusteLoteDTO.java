package com.dcriar.api.dto.response.stock;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

/**
 * Representa um item derivado de um lote que pode ter seu valor atualizado por um ajuste.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemImpactadoAjusteLoteDTO {

    @Schema(description = "ID do item impactado.", example = "18")
    private Long id;

    @Schema(description = "Tipo do item impactado.", example = "RETALHO")
    private String tipoItem;

    @Schema(description = "Descrição amigável do item.", example = "Retalho originado do lote #15")
    private String descricao;

    @Schema(description = "Saldo atual do item impactado.", example = "0.0700")
    private BigDecimal saldoAtual;

    @Schema(description = "Valor atual do item impactado.", example = "7.00")
    private BigDecimal valorAtual;

    @Schema(description = "Valor projetado do item impactado após o ajuste.", example = "8.16")
    private BigDecimal valorProjetado;

    @Schema(description = "Indica se o item deveria vir selecionado por padrão na confirmação.", example = "true")
    private boolean selecionadoPorPadrao;
}
