package com.dcriar.api.validation.validator;

import com.dcriar.api.dto.request.production.OrdemDeConsumoRequestDTO;
import com.dcriar.api.validation.annotation.ValidOrdemDeConsumoRequest;

public class OrdemDeConsumoRequestValidator extends BaseValidator<ValidOrdemDeConsumoRequest, OrdemDeConsumoRequestDTO> {

    @Override
    protected void validate(OrdemDeConsumoRequestDTO dto) {
        addViolationIf(dto.getProdutoId() == null, "O ID do produto é obrigatório.", "produtoId");
        addViolationIf(dto.getLoteId() == null, "O ID do lote é obrigatório.", "loteId");
        addViolationIf(dto.getQuantidadeProduzida() == null, "A quantidade produzida é obrigatória.", "quantidadeProduzida");
        if (dto.getQuantidadeProduzida() != null) {
            addViolationIf(dto.getQuantidadeProduzida() <= 0, "A quantidade produzida deve ser um número positivo.", "quantidadeProduzida");
        }
    }
}
