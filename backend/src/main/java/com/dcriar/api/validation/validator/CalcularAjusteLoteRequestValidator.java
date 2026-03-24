package com.dcriar.api.validation.validator;

import com.dcriar.api.dto.request.stock.CalcularAjusteLoteRequestDTO;
import com.dcriar.api.validation.annotation.ValidCalcularAjusteLoteRequest;
import com.dcriar.domain.stock.entity.enums.TipoOperacaoAjusteLote;

import java.math.BigDecimal;

/**
 * Validador do DTO de cálculo de ajuste operacional de lote.
 */
public class CalcularAjusteLoteRequestValidator extends BaseValidator<ValidCalcularAjusteLoteRequest, CalcularAjusteLoteRequestDTO> {

    @Override
    protected void validate(CalcularAjusteLoteRequestDTO dto) {
        addViolationIf(dto.getTipoOperacao() == null, "O tipo da operação é obrigatório.", "tipoOperacao");
        addViolationIf(dto.getQuantidade() == null || dto.getQuantidade().compareTo(BigDecimal.ZERO) <= 0,
                "A quantidade do ajuste é obrigatória e deve ser maior que zero.", "quantidade");
        addViolationIf(dto.getMotivo() == null || dto.getMotivo().isBlank(),
                "O motivo do ajuste é obrigatório.", "motivo");

        if (dto.getTipoOperacao() == TipoOperacaoAjusteLote.AJUSTE) {
            addViolationIf(dto.getDirecao() == null,
                    "A direção do ajuste é obrigatória para a operação de ajuste.", "direcao");
        }

        if (dto.getTipoOperacao() == TipoOperacaoAjusteLote.PERDA_DESCARTE) {
            addViolationIf(dto.getDirecao() != null,
                    "Perda/Descarte não aceita direção manual.", "direcao");
        }
    }
}
