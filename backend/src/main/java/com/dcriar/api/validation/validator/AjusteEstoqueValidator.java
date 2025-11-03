package com.dcriar.api.validation.validator;

import com.dcriar.api.dto.request.product.AjusteEstoqueRequestDTO;
import com.dcriar.api.validation.annotation.ValidAjusteEstoque;

/**
 * Validador para o DTO {@link AjusteEstoqueRequestDTO}, acionado pela anotação {@link ValidAjusteEstoque}.
 * <p>
 * Este validador verifica se os campos essenciais para um ajuste de estoque estão presentes:
 * <ul>
 *     <li>O {@code produtoId} não pode ser nulo.</li>
 *     <li>O {@code canalVendaId} não pode ser nulo.</li>
 *     <li>A {@code quantidade} não pode ser nula nem zero.</li>
 * </ul>
 */
public class AjusteEstoqueValidator extends BaseValidator<ValidAjusteEstoque, AjusteEstoqueRequestDTO> {

    @Override
    protected void validate(AjusteEstoqueRequestDTO dto) {
        addViolationIf(dto.getProdutoId() == null, "O ID do produto é obrigatório.", "produtoId");
        addViolationIf(dto.getCanalVendaId() == null, "O ID do canal de venda é obrigatório.", "canalVendaId");
        addViolationIf(dto.getQuantidade() == null || dto.getQuantidade() == 0, "A quantidade do ajuste deve ser diferente de zero.", "quantidade");
    }
}
