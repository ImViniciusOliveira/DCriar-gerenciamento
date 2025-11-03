package com.dcriar.api.validation.validator;

import com.dcriar.api.dto.request.production.MargensRequestDTO;
import com.dcriar.api.validation.annotation.ValidMargensRequest;

import java.math.BigDecimal;

/**
 * Validador para o DTO {@link MargensRequestDTO}, acionado pela anotação {@link ValidMargensRequest}.
 * <p>
 * Este validador verifica se, caso o objeto não seja nulo, todos os seus campos
 * (superior, inferior, esquerda, direita) são valores não-negativos (maiores ou iguais a zero).
 * A verificação se o DTO em si é obrigatório é feita em outro lugar (ex: {@link OrdemDeCorteRequestValidator}).
 */
public class MargensRequestValidator extends BaseValidator<ValidMargensRequest, MargensRequestDTO> {

    @Override
    protected void validate(MargensRequestDTO dto) {
        addViolationIf(isPositiveOrZero(dto.getSuperior()), "A margem superior deve ser um valor positivo ou zero.", "superior");
        addViolationIf(isPositiveOrZero(dto.getInferior()), "A margem inferior deve ser um valor positivo ou zero.", "inferior");
        addViolationIf(isPositiveOrZero(dto.getEsquerda()), "A margem esquerda deve ser um valor positivo ou zero.", "esquerda");
        addViolationIf(isPositiveOrZero(dto.getDireita()), "A margem direita deve ser um valor positivo ou zero.", "direita");
    }

    private boolean isPositiveOrZero(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) < 0;
    }
}
