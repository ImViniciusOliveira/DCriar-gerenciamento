package com.dcriar.api.validation.validator;

import com.dcriar.api.dto.request.production.SimulacaoConsumoDiretoRequestDTO;
import com.dcriar.api.validation.annotation.ValidSimulacaoConsumoDiretoRequest;

public class SimulacaoConsumoDiretoRequestValidator extends BaseValidator<ValidSimulacaoConsumoDiretoRequest, SimulacaoConsumoDiretoRequestDTO> {

    @Override
    protected void validate(SimulacaoConsumoDiretoRequestDTO dto) {
        addViolationIf(dto.getProdutoId() == null, "O ID do produto é obrigatório.", "produtoId");
        addViolationIf(dto.getQuantidade() == null, "A quantidade é obrigatória.", "quantidade");
        if (dto.getQuantidade() != null) {
            addViolationIf(dto.getQuantidade() <= 0, "A quantidade deve ser um número positivo.", "quantidade");
        }
    }
}
