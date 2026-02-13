package com.dcriar.api.validation.validator;

import com.dcriar.api.dto.request.stock.LoteMateriaPrimaRequestDTO;
import com.dcriar.api.validation.annotation.ValidLoteMateriaPrimaRequest;
import com.dcriar.domain.stock.entity.enums.UnidadeDeMedida;

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

        validateBigDecimal(dto.getQuantidadeInicial(), "quantidadeInicial", "A quantidade inicial");
        validateBigDecimal(dto.getCustoTotalLote(), "custoTotalLote", "O custo total do lote");

        // Validação condicional para METRO_LINEAR
        if (unidadeDeEstoque == UnidadeDeMedida.METRO_LINEAR) {
            Map<String, Object> atributos = dto.getAtributos();
            addViolationIf(atributos == null || !atributos.containsKey("larguraMm") || atributos.get("larguraMm") == null, "Para a unidade de estoque METRO_LINEAR, o atributo 'larguraMm' é obrigatório.", "atributos");

            if (atributos != null && atributos.containsKey("larguraMm") && atributos.get("larguraMm") != null) {
                Object larguraValue = atributos.get("larguraMm");
                boolean isInvalidNumber = true;
                if (larguraValue instanceof Number) {
                    if (((Number) larguraValue).doubleValue() > 0) {
                        isInvalidNumber = false;
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
