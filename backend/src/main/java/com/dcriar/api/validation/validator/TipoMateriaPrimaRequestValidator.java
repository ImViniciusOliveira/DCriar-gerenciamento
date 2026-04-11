package com.dcriar.api.validation.validator;

import com.dcriar.api.dto.request.stock.TipoMateriaPrimaRequestDTO;
import com.dcriar.api.validation.annotation.ValidTipoMateriaPrimaRequest;

import java.math.BigDecimal;

/**
 * Validador para o DTO {@link TipoMateriaPrimaRequestDTO}, acionado pela anotação {@link ValidTipoMateriaPrimaRequest}.
 * <p>
 * Este validador verifica se os campos essenciais para a definição de um tipo de matéria-prima estão presentes e são válidos:
 * <ul>
 *     <li>O {@code nome} não pode ser nulo, vazio ou exceder 150 caracteres.</li>
 *     <li>A {@code unidadeDeConsumo} não pode ser nula.</li>
 * </ul>
 */
public class TipoMateriaPrimaRequestValidator extends BaseValidator<ValidTipoMateriaPrimaRequest, TipoMateriaPrimaRequestDTO> {

    private static final int MAX_NOME_LENGTH = 150;

    @Override
    protected void validate(TipoMateriaPrimaRequestDTO dto) {
        String nome = dto.getNome();
        addViolationIf(nome == null || nome.isBlank(), "O nome do tipo de matéria-prima é obrigatório.", "nome");
        addViolationIf(nome != null && nome.length() > MAX_NOME_LENGTH, String.format("O nome não pode ter mais de %d caracteres.", MAX_NOME_LENGTH), "nome");

        addViolationIf(dto.getUnidadeDeConsumo() == null, "A unidade de consumo é obrigatória.", "unidadeDeConsumo");

        BigDecimal estoqueCritico = dto.getEstoqueCritico();
        BigDecimal estoqueAceitavel = dto.getEstoqueAceitavel();

        addViolationIf(estoqueCritico != null && estoqueCritico.compareTo(BigDecimal.ZERO) < 0,
                "O estoque crítico não pode ser negativo.", "estoqueCritico");
        addViolationIf(estoqueAceitavel != null && estoqueAceitavel.compareTo(BigDecimal.ZERO) < 0,
                "O estoque aceitável não pode ser negativo.", "estoqueAceitavel");
    }
}
