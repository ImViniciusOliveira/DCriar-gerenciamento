package com.dcriar.api.validation.validator;

import com.dcriar.api.dto.request.product.ProdutoRequestDTO;
import com.dcriar.api.validation.annotation.ValidProdutoRequest;

import java.math.BigDecimal;

/**
 * Validador para o DTO {@link ProdutoRequestDTO}, acionado pela anotação {@link ValidProdutoRequest}.
 * <p>
 * Este validador aplica regras de negócio condicionais com base no {@code tipoProduto}:
 * <ul>
 *     <li>Valida campos comuns a todos os tipos (nome, SKU, etc.).</li>
 *     <li>Se o tipo for {@code CORTE}, exige {@code cor} e {@code dimensoes}.</li>
 *     <li>Se o tipo for {@code CONSUMO}, exige {@code codigoFabricante}.</li>
 * </ul>
 */
public class ProdutoRequestValidator extends BaseValidator<ValidProdutoRequest, ProdutoRequestDTO> {

    @Override
    protected void validate(ProdutoRequestDTO dto) {
        // Validações comuns a todos os tipos de produto
        addViolationIf(dto.getNome() == null || dto.getNome().isBlank(), "O nome do produto é obrigatório.", "nome");
        addViolationIf(dto.getSku() == null || dto.getSku().isBlank(), "O SKU do produto é obrigatório.", "sku");
        addViolationIf(dto.getUnidadesPorProduto() == null || dto.getUnidadesPorProduto() <= 0, "A quantidade de unidades por produto deve ser um número positivo.", "unidadesPorProduto");
        addViolationIf(dto.getTipoMateriaPrimaId() == null, "O ID do tipo de matéria-prima é obrigatório.", "tipoMateriaPrimaId");

        String tipoProduto = dto.getTipoProduto();

        if (tipoProduto == null || tipoProduto.isBlank()) {
            addViolationIf(true, "O tipo de produto é obrigatório.", "tipoProduto");
            return; // Encerra a validação se o tipo for nulo para evitar NullPointerException
        }

        // Validações condicionais baseadas no tipo do produto
        if ("CORTE".equals(tipoProduto)) {
            addViolationIf(dto.getCor() == null || dto.getCor().isBlank(), "A cor do produto é obrigatória.", "cor");
            addViolationIf(dto.getDimensoes() == null, "As dimensões são obrigatórias.", "dimensoes");
            if (dto.getDimensoes() != null) {
                addViolationIf(dto.getDimensoes().getLarguraCm() == null || dto.getDimensoes().getLarguraCm().compareTo(BigDecimal.ZERO) <= 0, "A largura deve ser um número positivo.", "dimensoes.larguraCm");
                addViolationIf(dto.getDimensoes().getComprimentoCm() == null || dto.getDimensoes().getComprimentoCm().compareTo(BigDecimal.ZERO) <= 0, "O comprimento deve ser um número positivo.", "dimensoes.comprimentoCm");
            }
        } else if ("CONSUMO".equals(tipoProduto)) {
            addViolationIf(dto.getCodigoFabricante() == null || dto.getCodigoFabricante().isBlank(), "O código do fabricante é obrigatório.", "codigoFabricante");
        } else {
            addViolationIf(true, "O tipo de produto fornecido é inválido. Use 'CORTE' ou 'CONSUMO'.", "tipoProduto");
        }
    }
}
