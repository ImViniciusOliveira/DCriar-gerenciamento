package com.dcriar.api.dto.request.stock;

import com.dcriar.api.validation.annotation.ValidMovimentacaoRequest;
import com.dcriar.domain.stock.entity.enums.TipoMovimentacao;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

/**
 * Data Transfer Object (DTO) para registrar uma nova movimentação em um Lote de Matéria-Prima.
 * <p>
 * Este DTO é usado para registrar entradas (ex: devolução de sobras) ou saídas
 * (ex: consumo em produção, perdas) em um lote específico. A validação dos campos
 * é garantida pela anotação {@link ValidMovimentacaoRequest}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ValidMovimentacaoRequest
public class MovimentacaoRequestDTO {

    /**
     * O tipo da movimentação a ser registrada (ex: SAIDA_PRODUCAO, ENTRADA_DEVOLUCAO).
     */
    @Schema(description = "O tipo da movimentação a ser registrada.", example = "SAIDA_PRODUCAO", requiredMode = Schema.RequiredMode.REQUIRED)
    private TipoMovimentacao tipo;

    /**
     * A quantidade a ser movimentada.
     * <p>
     * Use um valor positivo para entradas e um valor negativo para saídas.
     */
    @Schema(description = "Quantidade a ser movimentada. Use valor positivo para entradas e negativo para saídas.", example = "-1.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal quantidade;

    /**
     * Um motivo ou observação para a movimentação, para fins de auditoria.
     */
    @Schema(description = "Um motivo ou observação para a movimentação, para fins de auditoria.", example = "Uso na produção do pedido #123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String motivo;
}
