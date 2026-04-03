package com.dcriar.api.dto.response.product;

import com.dcriar.domain.product.entity.enums.TipoMovimentacaoProduto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO de resposta para a listagem consolidada do histórico de movimentações de estoque.
 * Cada instância representa uma linha da tabela de histórico, trazendo os dados
 * da movimentação e um resumo do produto associado.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoricoEstoqueConsolidadoResponseDTO {

    @Schema(description = "ID da movimentação.", example = "2041")
    private Long id;

    @Schema(description = "Data e hora da movimentação.", example = "2026-04-02T07:42:00")
    private LocalDateTime data;

    @Schema(description = "Tipo da movimentação.", example = "AJUSTE_MANUAL")
    private TipoMovimentacaoProduto tipo;

    @Schema(description = "Descrição amigável do tipo da movimentação.", example = "Ajuste manual")
    private String tipoDescricao;

    @Schema(description = "Quantidade movimentada.", example = "-7")
    private Integer quantidade;

    @Schema(description = "Motivo registrado para a movimentação.", example = "Venda #8841")
    private String motivo;

    @Schema(description = "ID do produto associado.", example = "11")
    private Long produtoId;

    @Schema(description = "Nome do produto associado.", example = "Adesivo Holográfico 10x10cm")
    private String produtoNome;

    @Schema(description = "SKU do produto associado.", example = "ADH-10X10-HOLO")
    private String produtoSku;

    @Schema(description = "Nome do produto registrado no momento da movimentação.", example = "Adesivo Holográfico 10x10cm")
    private String produtoNomeSnapshot;

    @Schema(description = "SKU do produto registrado no momento da movimentação.", example = "ADH-10X10-HOLO")
    private String produtoSkuSnapshot;

    @Schema(description = "ID da ordem de produção de origem, quando aplicável.", example = "11")
    private Long ordemProducaoId;

    @Schema(description = "ID da venda de origem, quando aplicável.", example = "6")
    private Long vendaId;
}
