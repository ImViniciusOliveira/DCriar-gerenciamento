package com.dcriar.api.validation.validator;

import com.dcriar.api.dto.request.stock.LoteMateriaPrimaRequestDTO;
import com.dcriar.api.validation.annotation.ValidLoteMateriaPrimaRequest;
import com.dcriar.domain.common.util.LogicalMapKeySupport;
import com.dcriar.domain.stock.entity.enums.UnidadeDeMedida;
import com.dcriar.exception.custom.LogicalMapKeyInvalidaException;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Validador para o DTO {@link LoteMateriaPrimaRequestDTO}, acionado pela anotação {@link ValidLoteMateriaPrimaRequest}.
 * <p>
 * Este validador verifica as regras de negócio para a criação de um novo lote de matéria-prima:
 * <ul>
 *     <li>Campos básicos como {@code tipoMateriaPrimaId} e {@code unidadeDeEstoque} são obrigatórios.</li>
 *     <li>Os campos {@code quantidadeInicial} e {@code custoTotalLote} devem ser valores positivos e dentro dos limites de precisão do banco de dados.</li>
 *     <li>Se a {@code unidadeDeEstoque} for METRO_LINEAR, o mapa de {@code atributos} deve conter a chave 'larguraMm' com um valor numérico positivo.</li>
 * </ul>
 */
public class LoteMateriaPrimaRequestValidator extends BaseValidator<ValidLoteMateriaPrimaRequest, LoteMateriaPrimaRequestDTO> {

    private static final int MAX_INTEGER_DIGITS = 15;
    private static final int MAX_FRACTION_DIGITS = 4;

    @Override
    protected void validate(LoteMateriaPrimaRequestDTO dto) {
        addViolationIf(dto.getTipoMateriaPrimaId() == null, "O ID do tipo de matéria-prima é obrigatório.", "tipoMateriaPrimaId");

        UnidadeDeMedida unidadeDeEstoque = dto.getUnidadeDeEstoque();
        addViolationIf(unidadeDeEstoque == null, "A unidade de estoque é obrigatória.", "unidadeDeEstoque");

        UnidadeDeMedida unidadeCadastroEstoque = dto.getUnidadeCadastroEstoque();
        if (unidadeCadastroEstoque != null) {
            addViolationIf(unidadeDeEstoque == null, "A unidade de estoque deve ser informada antes da unidade de cadastro.", "unidadeCadastroEstoque");
        }

        validateBigDecimal(dto.getQuantidadeInicial(), "quantidadeInicial", "A quantidade inicial");
        validateBigDecimal(dto.getCustoTotalLote(), "custoTotalLote", "O custo total do lote");

        try {
            LogicalMapKeySupport.validateNoLogicalDuplicates(dto.getAtributos(), "atributos");
        } catch (LogicalMapKeyInvalidaException ex) {
            addViolationIf(true, ex.getMessage(), ex.getFieldPath());
        }

        // Validação condicional para unidades geométricas
        if (unidadeDeEstoque != null && unidadeDeEstoque.exigeLarguraMmNoLote()) {
            Map<String, Object> atributos = dto.getAtributos();
            addViolationIf(
                    atributos == null || !LogicalMapKeySupport.containsLogicalKey(atributos, "larguraMm")
                            || LogicalMapKeySupport.getLogicalValue(atributos, "larguraMm") == null,
                    String.format("Para a unidade de estoque %s, o atributo 'larguraMm' é obrigatório.", unidadeDeEstoque.name()),
                    "atributos"
            );

            if (atributos != null && LogicalMapKeySupport.containsLogicalKey(atributos, "larguraMm")
                    && LogicalMapKeySupport.getLogicalValue(atributos, "larguraMm") != null) {
                Object larguraValue = LogicalMapKeySupport.getLogicalValue(atributos, "larguraMm");
                boolean isInvalidNumber = true;
                
                if (larguraValue instanceof Number) {
                    if (((Number) larguraValue).doubleValue() > 0) {
                        isInvalidNumber = false;
                    }
                } else if (larguraValue instanceof String) {
                    try {
                        // Tenta converter String para número para ser robusto
                        if (Double.parseDouble((String) larguraValue) > 0) {
                            isInvalidNumber = false;
                        }
                    } catch (NumberFormatException e) {
                        // Não é um número válido
                    }
                }

                addViolationIf(isInvalidNumber, "O atributo 'larguraMm' deve ser um número positivo.", "atributos");
            }
        }
    }

    private void validateBigDecimal(BigDecimal value, String fieldName, String fieldDescription) {
        if (value == null) {
            addViolationIf(true, fieldDescription + " é obrigatório.", fieldName);
            return;
        }

        addViolationIf(value.compareTo(BigDecimal.ZERO) <= 0, fieldDescription + " deve ser um valor positivo.", fieldName);

        if (value.scale() > MAX_FRACTION_DIGITS) {
            addViolationIf(true, String.format("%s não pode ter mais de %d casas decimais.", fieldDescription, MAX_FRACTION_DIGITS), fieldName);
        }

        if (value.precision() - value.scale() > MAX_INTEGER_DIGITS) {
            addViolationIf(true, String.format("%s excede o número máximo de %d dígitos inteiros permitidos.", fieldDescription, MAX_INTEGER_DIGITS), fieldName);
        }
    }
}
