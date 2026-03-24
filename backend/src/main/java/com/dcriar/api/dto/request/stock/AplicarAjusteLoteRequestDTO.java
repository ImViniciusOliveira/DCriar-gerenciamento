package com.dcriar.api.dto.request.stock;

import com.dcriar.api.validation.annotation.ValidAplicarAjusteLoteRequest;
import com.dcriar.domain.stock.entity.enums.DirecaoAjusteLote;
import com.dcriar.domain.stock.entity.enums.TipoOperacaoAjusteLote;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO de entrada para aplicar um ajuste operacional em um lote.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ValidAplicarAjusteLoteRequest
public class AplicarAjusteLoteRequestDTO {

    @Schema(description = "Tipo operacional do ajuste.", example = "AJUSTE", requiredMode = Schema.RequiredMode.REQUIRED)
    private TipoOperacaoAjusteLote tipoOperacao;

    @Schema(description = "Direção do ajuste. Obrigatória apenas para AJUSTE.", example = "RETIRAR")
    private DirecaoAjusteLote direcao;

    @Schema(description = "Quantidade informada pelo usuário na unidade de cadastro do lote.", example = "20.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal quantidade;

    @Schema(description = "Motivo do ajuste para auditoria.", example = "Correção após contagem física.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String motivo;

    @Schema(description = "IDs dos itens impactados que terão o valor atualizado.", example = "[14,18]")
    private List<Long> idsItensImpactadosAtualizados;
}
