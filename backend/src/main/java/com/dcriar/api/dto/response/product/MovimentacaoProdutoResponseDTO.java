package com.dcriar.api.dto.response.product;

import com.dcriar.domain.product.entity.enums.TipoMovimentacaoProduto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * Data Transfer Object (DTO) que representa a resposta de uma movimentação de estoque de produto.
 * <p>
 * Cada objeto deste DTO representa uma entrada ou saída no histórico (livro-razão)
 * do estoque mestre de um produto acabado, detalhando o tipo, a quantidade e o motivo da movimentação.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovimentacaoProdutoResponseDTO {

    /**
     * O ID único da movimentação de estoque do produto.
     */
    @Schema(description = "ID único da movimentação.", example = "1")
    private Long id;

    /**
     * A data e hora em que a movimentação foi registrada.
     */
    @Schema(description = "Data e hora da movimentação.", example = "2025-09-22T03:13:23.522Z")
    private OffsetDateTime data;

    /**
     * O tipo da movimentação do estoque (ex: ENTRADA_PRODUCAO, SAIDA_VENDA).
     */
    @Schema(description = "Tipo da movimentação do estoque.", example = "ENTRADA_PRODUCAO")
    private TipoMovimentacaoProduto tipo;

    /**
     * A quantidade de unidades do produto que foi movimentada.
     * <p>
     * O valor é positivo para entradas (ex: produção) e negativo para saídas (ex: venda).
     */
    @Schema(description = "Quantidade movimentada. Positiva para entradas, negativa para saídas.", example = "100")
    private Integer quantidade;

    /**
     * O motivo ou observação associado à movimentação, como o ID da ordem de corte ou da venda.
     */
    @Schema(description = "Motivo ou observação da movimentação.", example = "Produzido via Ordem de Corte ID: #123")
    private String motivo;
}
