package com.dcriar.api.validation.validator;

import com.dcriar.api.dto.request.product.ProdutoRequestDTO;
import com.dcriar.api.validation.annotation.ValidProdutoRequest;

/**
 * Validador para o DTO {@link ProdutoRequestDTO}, acionado pela anotação {@link ValidProdutoRequest}.
 * <p>
 * Este validador verifica se os campos essenciais da requisição de produto estão presentes e são válidos:
 * <ul>
 *     <li>O {@code nome} não pode ser nulo ou vazio.</li>
 *     <li>O {@code sku} não pode ser nulo ou vazio.</li>
 *     <li>A {@code cor} não pode ser nula ou vazia.</li>
 *     <li>O {@code tipoMateriaPrimaId} não pode ser nulo.</li>
 *     <li>As {@code unidadesPorProduto} devem ser um número positivo.</li>
 *     <li>As {@code dimensoesUnitarias} não podem ser nulas (a validação interna é delegada).</li>
 * </ul>
 */
public class ProdutoRequestValidator extends BaseValidator<ValidProdutoRequest, ProdutoRequestDTO> {

    @Override
    protected void validate(ProdutoRequestDTO dto) {
        addViolationIf(dto.getNome() == null || dto.getNome().isBlank(), "O nome do produto é obrigatório.", "nome");
        addViolationIf(dto.getSku() == null || dto.getSku().isBlank(), "O SKU do produto é obrigatório.", "sku");
        addViolationIf(dto.getCor() == null || dto.getCor().isBlank(), "A cor do produto é obrigatória.", "cor");
        addViolationIf(dto.getUnidadesPorProduto() == null || dto.getUnidadesPorProduto() <= 0, "A quantidade de unidades por produto deve ser um número positivo.", "unidadesPorProduto");
        addViolationIf(dto.getTipoMateriaPrimaId() == null, "O ID do tipo de matéria-prima é obrigatório.", "tipoMateriaPrimaId");
        addViolationIf(dto.getDimensoesUnitarias() == null, "As dimensões unitárias são obrigatórias.", "dimensoesUnitarias");
    }
}
