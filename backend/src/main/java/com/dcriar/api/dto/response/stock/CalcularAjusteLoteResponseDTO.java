package com.dcriar.api.dto.response.stock;

import com.dcriar.domain.stock.entity.enums.DirecaoAjusteLote;
import com.dcriar.domain.stock.entity.enums.ContextoItensImpactadosAjusteLote;
import com.dcriar.domain.stock.entity.enums.TipoOperacaoAjusteLote;
import com.dcriar.domain.stock.entity.enums.TipoMovimentacao;
import com.dcriar.domain.stock.entity.enums.UnidadeDeMedida;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO de saída com o preview do impacto de um ajuste operacional em um lote.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalcularAjusteLoteResponseDTO {

    @Schema(description = "ID do lote ajustado.", example = "15")
    private Long loteId;

    @Schema(description = "Tipo operacional do ajuste.", example = "AJUSTE")
    private TipoOperacaoAjusteLote tipoOperacao;

    @Schema(description = "Descrição amigável do tipo operacional.", example = "Ajuste")
    private String tipoOperacaoDescricao;

    @Schema(description = "Direção do ajuste, quando aplicável.", example = "RETIRAR")
    private DirecaoAjusteLote direcao;

    @Schema(description = "Descrição amigável da direção do ajuste, quando aplicável.", example = "Retirar")
    private String direcaoDescricao;

    @Schema(description = "Unidade de apresentação do lote.", example = "METRO_QUADRADO")
    private UnidadeDeMedida unidadeApresentacao;

    @Schema(description = "Símbolo da unidade do lote.", example = "m²")
    private String unidadeSimbolo;

    @Schema(description = "Saldo atual do lote.", example = "50.0000")
    private BigDecimal saldoAtual;

    @Schema(description = "Saldo projetado do lote após o ajuste.", example = "30.0000")
    private BigDecimal saldoProjetado;

    @Schema(description = "Valor atual derivado do lote.", example = "50.00")
    private BigDecimal valorAtualLote;

    @Schema(description = "Valor projetado do lote após o ajuste.", example = "30.00")
    private BigDecimal valorProjetadoLote;

    @Schema(description = "Custo unitário atual do lote.", example = "1.00000000")
    private BigDecimal custoUnitarioAtual;

    @Schema(description = "Custo unitário projetado do lote.", example = "1.00000000")
    private BigDecimal custoUnitarioProjetado;

    @Schema(description = "Tipo técnico de movimentação que será gerado na aplicação.", example = "PERDA_DESCARTE")
    private TipoMovimentacao tipoMovimentacaoGerada;

    @Schema(description = "Quantidade técnica da movimentação que será registrada.", example = "-20.0000")
    private BigDecimal quantidadeMovimentacaoGerada;

    @Schema(description = "Contexto semântico da lista de itens impactados.", example = "SEM_RETALHOS_VINCULADOS")
    private ContextoItensImpactadosAjusteLote contextoItensImpactados;

    @Schema(description = "Itens derivados impactados pelo ajuste.", implementation = ItemImpactadoAjusteLoteDTO.class)
    private List<ItemImpactadoAjusteLoteDTO> itensImpactados;
}
