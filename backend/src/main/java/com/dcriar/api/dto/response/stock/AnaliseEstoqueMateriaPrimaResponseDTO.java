package com.dcriar.api.dto.response.stock;

import com.dcriar.domain.stock.entity.enums.StatusAnaliseMateriaPrima;
import com.dcriar.domain.stock.entity.enums.UnidadeDeMedida;
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
public class AnaliseEstoqueMateriaPrimaResponseDTO {

    @Schema(description = "ID do tipo de matéria-prima analisado.", example = "10")
    private Long tipoMateriaPrimaId;

    @Schema(description = "Nome do tipo de matéria-prima.", example = "Adesivo BOPP Transparente")
    private String nomeTipoMateriaPrima;

    @Schema(description = "Unidade principal de consumo do tipo.", example = "METRO_QUADRADO")
    private UnidadeDeMedida unidadeDeConsumo;

    @Schema(description = "Descrição amigável da unidade principal.", example = "Metro Quadrado")
    private String unidadeDescricao;

    @Schema(description = "Símbolo da unidade principal.", example = "m²")
    private String unidadeSimbolo;

    @Schema(description = "Classificação operacional derivada da unidade do tipo.", example = "CORTE")
    private String tipoProdutoCompativel;

    @Schema(description = "Saldo consolidado dos lotes principais, convertido para a unidade principal do tipo.", example = "150.5000")
    private BigDecimal saldoLotesPrincipais;

    @Schema(description = "Saldo consolidado dos retalhos, convertido para a unidade principal do tipo.", example = "12.7500")
    private BigDecimal saldoRetalhos;

    @Schema(description = "Saldo total consolidado do tipo, convertido para a unidade principal do tipo.", example = "163.2500")
    private BigDecimal saldoTotal;

    @Schema(description = "Saldo efetivamente considerado na análise, conforme a política de retalhos aplicada.", example = "150.5000")
    private BigDecimal saldoConsiderado;

    @Schema(description = "Quantidade de lotes principais que compõem a análise.", example = "3")
    private long quantidadeLotesPrincipais;

    @Schema(description = "Quantidade de retalhos que compõem a análise.", example = "8")
    private long quantidadeRetalhos;

    @Schema(description = "Limite crítico configurado para o tipo, na unidade principal.", example = "30.0000")
    private BigDecimal estoqueCritico;

    @Schema(description = "Limite aceitável configurado para o tipo, na unidade principal.", example = "80.0000")
    private BigDecimal estoqueAceitavel;

    @Schema(description = "Percentual de risco operacional entre a faixa aceitável e a crítica. 0 significa saudável e 100 significa crítico.", example = "41.25")
    private BigDecimal percentualRisco;

    @Schema(description = "Status operacional calculado para a matéria-prima.", example = "ATENCAO")
    private StatusAnaliseMateriaPrima statusAnalise;
}
