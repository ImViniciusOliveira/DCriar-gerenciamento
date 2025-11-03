package com.dcriar.api.validation.validator;

import com.dcriar.api.dto.request.product.MovimentacaoEstoqueProdutoRequestDTO;
import com.dcriar.api.validation.annotation.ValidMovimentacaoEstoqueProdutoRequest;

public class MovimentacaoEstoqueProdutoRequestValidator extends BaseValidator<ValidMovimentacaoEstoqueProdutoRequest, MovimentacaoEstoqueProdutoRequestDTO> {

    @Override
    protected void validate(MovimentacaoEstoqueProdutoRequestDTO dto) {
        addViolationIf(dto.getProdutoId() == null, "O ID do produto é obrigatório.", "produtoId");
        addViolationIf(dto.getTipo() == null || dto.getTipo().isBlank(), "O tipo da movimentação é obrigatório.", "tipo");
        addViolationIf(dto.getQuantidade() == null, "A quantidade é obrigatória.", "quantidade");
    }
}
