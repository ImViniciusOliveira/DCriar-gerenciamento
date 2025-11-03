package com.dcriar.api.validation.validator;

import com.dcriar.api.dto.request.production.CorteRealizadoRequestDTO;
import com.dcriar.api.validation.annotation.ValidCorteRealizadoRequest;

import java.math.BigDecimal;

public class CorteRealizadoRequestValidator extends BaseValidator<ValidCorteRealizadoRequest, CorteRealizadoRequestDTO> {

    @Override
    protected void validate(CorteRealizadoRequestDTO dto) {
        addViolationIf(dto.getOrdemDeProducaoId() == null, "O ID da ordem de produção é obrigatório.", "ordemDeProducaoId");
        addViolationIf(dto.getLarguraCm() == null, "A largura do corte é obrigatória.", "larguraCm");
        if (dto.getLarguraCm() != null) {
            addViolationIf(dto.getLarguraCm().compareTo(BigDecimal.ZERO) < 0, "A largura do corte não pode ser negativa.", "larguraCm");
        }
        addViolationIf(dto.getComprimentoCm() == null, "O comprimento do corte é obrigatório.", "comprimentoCm");
        if (dto.getComprimentoCm() != null) {
            addViolationIf(dto.getComprimentoCm().compareTo(BigDecimal.ZERO) < 0, "O comprimento do corte não pode ser negativo.", "comprimentoCm");
        }
        addViolationIf(dto.getQuantidade() == null, "A quantidade é obrigatória.", "quantidade");
        if (dto.getQuantidade() != null) {
            addViolationIf(dto.getQuantidade() < 1, "A quantidade deve ser no mínimo 1.", "quantidade");
        }
        addViolationIf(dto.getTipo() == null || dto.getTipo().isBlank(), "O tipo de resultado do corte é obrigatório.", "tipo");
    }
}
