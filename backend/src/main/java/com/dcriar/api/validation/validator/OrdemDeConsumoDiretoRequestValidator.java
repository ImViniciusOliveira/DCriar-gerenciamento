package com.dcriar.api.validation.validator;

import com.dcriar.api.dto.request.production.OrdemDeConsumoDiretoRequestDTO;
import com.dcriar.api.validation.annotation.ValidOrdemDeConsumoDiretoRequest;

public class OrdemDeConsumoDiretoRequestValidator extends BaseValidator<ValidOrdemDeConsumoDiretoRequest, OrdemDeConsumoDiretoRequestDTO> {

    @Override
    protected void validate(OrdemDeConsumoDiretoRequestDTO dto) {
        addViolationIf(dto.getProdutoId() == null, "O ID do produto é obrigatório.", "produtoId");
        addViolationIf(dto.getLotesConsumidosIds() == null || dto.getLotesConsumidosIds().isEmpty(), "A lista de IDs de lotes consumidos é obrigatória.", "lotesConsumidosIds");
        addViolationIf(dto.getQuantidadeProduzida() == null, "A quantidade produzida é obrigatória.", "quantidadeProduzida");
        if (dto.getQuantidadeProduzida() != null) {
            addViolationIf(dto.getQuantidadeProduzida() <= 0, "A quantidade produzida deve ser um número positivo.", "quantidadeProduzida");
        }
    }
}
