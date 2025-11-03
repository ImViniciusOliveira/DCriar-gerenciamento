package com.dcriar.api.validation.validator;

import com.dcriar.api.dto.request.sales.ItemVendaRequestDTO;
import com.dcriar.api.validation.annotation.ValidItemVendaRequest;

/**
 * Validador para o DTO {@link ItemVendaRequestDTO}, acionado pela anotação {@link ValidItemVendaRequest}.
 * <p>
 * Este validador verifica se os campos essenciais de um item de venda estão presentes e são válidos:
 * <ul>
 *     <li>O {@code produtoId} não pode ser nulo.</li>
 *     <li>A {@code quantidade} deve ser um número positivo.</li>
 * </ul>
 */
public class ItemVendaRequestValidator extends BaseValidator<ValidItemVendaRequest, ItemVendaRequestDTO> {

    @Override
    protected void validate(ItemVendaRequestDTO dto) {
        addViolationIf(dto.getProdutoId() == null, "O ID do produto é obrigatório.", "produtoId");
        addViolationIf(dto.getQuantidade() == null || dto.getQuantidade() <= 0, "A quantidade deve ser um número positivo.", "quantidade");
    }
}
