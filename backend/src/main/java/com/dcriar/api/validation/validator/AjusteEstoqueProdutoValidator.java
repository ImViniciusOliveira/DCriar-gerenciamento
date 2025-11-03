package com.dcriar.api.validation.validator;

import com.dcriar.api.dto.request.product.AjusteEstoqueProdutoRequestDTO;
import com.dcriar.api.validation.annotation.ValidAjusteEstoqueProduto;

/**
 * Validador para o DTO {@link AjusteEstoqueProdutoRequestDTO}, acionado pela anotação {@link ValidAjusteEstoqueProduto}.
 * <p>
 * Este validador verifica se os campos essenciais para um ajuste de estoque de produto estão presentes:
 * <ul>
 *     <li>O {@code produtoId} não pode ser nulo.</li>
 *     <li>A {@code quantidade} não pode ser nula nem zero.</li>
 *     <li>O {@code motivo} não pode ser nulo ou vazio, pois é crucial para auditoria.</li>
 * </ul>
 */
public class AjusteEstoqueProdutoValidator extends BaseValidator<ValidAjusteEstoqueProduto, AjusteEstoqueProdutoRequestDTO> {

    @Override
    protected void validate(AjusteEstoqueProdutoRequestDTO dto) {
        addViolationIf(dto.getProdutoId() == null, "O ID do produto é obrigatório.", "produtoId");
        addViolationIf(dto.getQuantidade() == null || dto.getQuantidade() == 0, "A quantidade do ajuste deve ser diferente de zero.", "quantidade");
        addViolationIf(dto.getMotivo() == null || dto.getMotivo().isBlank(), "O motivo é obrigatório para ajustes manuais de estoque.", "motivo");
    }
}
