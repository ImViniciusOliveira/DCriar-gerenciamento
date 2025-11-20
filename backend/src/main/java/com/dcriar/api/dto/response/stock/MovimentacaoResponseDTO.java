package com.dcriar.api.dto.response.stock;

import com.dcriar.domain.stock.entity.enums.TipoMovimentacao;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object (DTO) que representa a resposta de uma movimentação de estoque.
 * <p>
 * Cada objeto deste DTO representa uma entrada ou saída no histórico (livro-razão)
 * de um lote de matéria-prima, detalhando o tipo, a quantidade e o motivo da movimentação.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovimentacaoResponseDTO {

    /**
     * O ID único da movimentação.
     */
    @Schema(description = "ID único da movimentação.", example = "1")
    private Long id;

    /**
     * A data e hora em que a movimentação foi registrada.
     */
    @Schema(description = "Data e hora em que a movimentação foi registada.")
    private LocalDateTime data;

    /**
     * O tipo da movimentação (ex: ENTRADA_COMPRA, SAIDA_PRODUCAO).
     */
    @Schema(description = "O tipo da movimentação.", example = "ENTRADA_COMPRA")
    private TipoMovimentacao tipo;

    /**
     * A quantidade que foi movimentada. O valor é positivo para entradas e negativo para saídas.
     */
    @Schema(description = "A quantidade que foi movimentada. Positiva para entradas, negativa para saídas.", example = "50.00")
    private BigDecimal quantidade;

    /**
     * O custo calculado por unidade base (ex: R$/cm², R$/ml) no momento da entrada.
     * <p>
     * Este campo só terá valor para movimentações do tipo ENTRADA_COMPRA.
     */
    @Schema(description = "O custo por unidade base no momento da entrada (ex: R$/cm²). Será nulo para outras movimentações.", example = "0.0007")
    private BigDecimal custoPorUnidadeBase;

    /**
     * O motivo ou observação registrado para a movimentação.
     */
    @Schema(description = "O motivo ou observação registado para a movimentação.", example = "Entrada inicial do lote no sistema.")
    private String motivo;
}
