package com.dcriar.api.dto.response.stock;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

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

    @Schema(description = "Identificador público do item impactado.", example = "RT-000018")
    private String identificadorPublico;

    @Schema(description = "Identificador público da origem imediata do item impactado.", nullable = true, example = "RT-000016")
    private String identificadorOrigemPublico;

    @Schema(description = "Nível do item na árvore a partir do lote ajustado.", example = "2")
    private Integer nivelArvore;

    @Schema(description = "Cadeia pública completa do item impactado na árvore.", example = "LT-000015 -> RT-000016 -> RT-000018")
    private String cadeiaPublica;

    @Schema(description = "Lista ordenada dos identificadores públicos da cadeia do item impactado.", example = "[\"LT-000015\", \"RT-000016\", \"RT-000018\"]")
    private List<String> cadeiaIdentificadoresPublicos;

    @Schema(description = "Descrição amigável do item.", example = "Retalho originado do lote #15")
    private String descricao;

    @Schema(description = "Saldo atual do item impactado.", example = "0.0700")
    private BigDecimal saldoAtual;

    @Schema(description = "Descrição formatada do saldo do item impactado.", example = "0,04m²")
    private String saldoDescricao;

    @Schema(description = "Descrição formatada das dimensões do item impactado, quando aplicável.", example = "20cm x 20cm")
    private String dimensaoDescricao;

    @Schema(description = "Valor atual do item impactado.", example = "7.00")
    private BigDecimal valorAtual;

    @Schema(description = "Valor projetado do item impactado após o ajuste.", example = "8.16")
    private BigDecimal valorProjetado;

    @Schema(description = "Indica se o item deveria vir selecionado por padrão na confirmação.", example = "true")
    private boolean selecionadoPorPadrao;
}
