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
 *     <li>Os campos {@code quantidadeInicial} e {@code custoTotalLote} devem ser valores positivos.</li>
 *     <li>Se a {@code unidadeDeEstoque} for METRO_LINEAR, o mapa de {@code atributos} deve conter a chave 'larguraMm' com um valor numérico positivo.</li>
 * </ul>
 */
public class LoteMateriaPrimaRequestValidator extends BaseValidator<ValidLoteMateriaPrimaRequest, LoteMateriaPrimaRequestDTO> {

    @Override
    protected void validate(LoteMateriaPrimaRequestDTO dto) {
        addViolationIf(dto.getTipoMateriaPrimaId() == null, "O ID do tipo de matéria-prima é obrigatório.", "tipoMateriaPrimaId");

        UnidadeDeMedida unidadeDeEstoque = dto.getUnidadeDeEstoque();
        addViolationIf(unidadeDeEstoque == null, "A unidade de estoque é obrigatória.", "unidadeDeEstoque");

        addViolationIf(dto.getQuantidadeInicial() == null || dto.getQuantidadeInicial().compareTo(BigDecimal.ZERO) <= 0, "A quantidade inicial deve ser um valor positivo.", "quantidadeInicial");
        addViolationIf(dto.getCustoTotalLote() == null || dto.getCustoTotalLote().compareTo(BigDecimal.ZERO) <= 0, "O custo total do lote deve ser um valor positivo.", "custoTotalLote");

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
}
