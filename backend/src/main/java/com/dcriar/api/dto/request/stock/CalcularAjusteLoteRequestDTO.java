package com.dcriar.api.dto.request.stock;

import com.dcriar.api.validation.annotation.ValidCalcularAjusteLoteRequest;
import com.dcriar.domain.stock.entity.enums.DirecaoAjusteLote;
import com.dcriar.domain.stock.entity.enums.TipoOperacaoAjusteLote;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

/**
 * DTO de entrada para calcular o impacto de um ajuste operacional em um lote.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ValidCalcularAjusteLoteRequest
public class CalcularAjusteLoteRequestDTO {

    @Schema(description = "Tipo operacional do ajuste.", example = "AJUSTE", requiredMode = Schema.RequiredMode.REQUIRED)
    private TipoOperacaoAjusteLote tipoOperacao;

    @Schema(description = "Direção do ajuste. Obrigatória apenas para AJUSTE.", example = "RETIRAR")
    private DirecaoAjusteLote direcao;

    @Schema(description = "Quantidade informada pelo usuário na unidade de cadastro do lote.", example = "20.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal quantidade;

    @Schema(description = "Motivo do ajuste para auditoria.", example = "Correção após contagem física.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String motivo;
}
