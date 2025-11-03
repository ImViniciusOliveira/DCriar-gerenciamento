package com.dcriar.api.validation.validator;

import com.dcriar.api.dto.request.product.EstoqueRequestDTO;
import com.dcriar.api.validation.annotation.ValidEstoqueRequest;

public class EstoqueRequestValidator extends BaseValidator<ValidEstoqueRequest, EstoqueRequestDTO> {

    @Override
    protected void validate(EstoqueRequestDTO dto) {
        addViolationIf(dto.getProdutoId() == null, "O ID do produto é obrigatório.", "produtoId");
        addViolationIf(dto.getCanalVendaId() == null, "O ID do canal de venda é obrigatório.", "canalVendaId");
        addViolationIf(dto.getQuantidade() == null, "A quantidade é obrigatória.", "quantidade");
        if (dto.getQuantidade() != null) {
            addViolationIf(dto.getQuantidade() <= 0, "A quantidade deve ser um número positivo.", "quantidade");
        }
    }
}
