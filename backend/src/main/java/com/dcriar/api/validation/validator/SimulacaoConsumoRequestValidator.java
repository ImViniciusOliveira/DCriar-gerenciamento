package com.dcriar.api.validation.validator;

import com.dcriar.api.dto.request.production.SimulacaoConsumoRequestDTO;
import com.dcriar.api.validation.annotation.ValidSimulacaoConsumoRequest;

public class SimulacaoConsumoRequestValidator extends BaseValidator<ValidSimulacaoConsumoRequest, SimulacaoConsumoRequestDTO> {

    @Override
    protected void validate(SimulacaoConsumoRequestDTO dto) {
        addViolationIf(dto.getProdutoId() == null, "O ID do produto é obrigatório.", "produtoId");
        addViolationIf(dto.getQuantidade() == null, "A quantidade é obrigatória.", "quantidade");
        addViolationIf(dto.getLoteId() == null, "O ID do lote é obrigatório.", "loteId");
        if (dto.getQuantidade() != null) {
            addViolationIf(dto.getQuantidade() <= 0, "A quantidade deve ser um número positivo.", "quantidade");
        }
    }
}
