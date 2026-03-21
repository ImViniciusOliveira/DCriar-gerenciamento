package com.dcriar.api.validation.validator;

import com.dcriar.api.dto.request.sales.ItemVendaRequestDTO;
import com.dcriar.api.validation.annotation.ValidItemVendaRequest;
import com.dcriar.domain.sales.entity.enums.TipoPrecoAplicado;

import java.math.BigDecimal;

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
        addViolationIf(dto.getPrecoAplicado() == null, "O preço aplicado é obrigatório.", "precoAplicado");
        addViolationIf(dto.getPrecoTotal() == null, "O preço total é obrigatório.", "precoTotal");
        addViolationIf(dto.getTipoPrecoAplicado() == null || dto.getTipoPrecoAplicado().isBlank(), "O tipo de preço aplicado é obrigatório.", "tipoPrecoAplicado");

        validarMonetario(dto.getPrecoAplicado(), "precoAplicado", 4);
        validarMonetario(dto.getPrecoTotal(), "precoTotal", 2);

        if (dto.getTipoPrecoAplicado() == null || dto.getTipoPrecoAplicado().isBlank()) {
            return;
        }

        if (TipoPrecoAplicado.isInvalid(dto.getTipoPrecoAplicado())) {
            addViolationIf(true, "Tipo de preço aplicado inválido.", "tipoPrecoAplicado");
            return;
        }
        TipoPrecoAplicado tipoPrecoAplicado = TipoPrecoAplicado.from(dto.getTipoPrecoAplicado());
        boolean motivoObrigatorio = tipoPrecoAplicado != TipoPrecoAplicado.PRECO_PADRAO;
        addViolationIf(motivoObrigatorio && (dto.getMotivoAlteracaoPreco() == null || dto.getMotivoAlteracaoPreco().isBlank()),
                "O motivo é obrigatório quando o preço padrão não é utilizado.",
                "motivoAlteracaoPreco");
    }

    private void validarMonetario(BigDecimal value, String fieldName, int maxFractionDigits) {
        if (value == null) {
            return;
        }

        addViolationIf(value.compareTo(BigDecimal.ZERO) < 0, "O valor monetário não pode ser negativo.", fieldName);
        addViolationIf(value.scale() > maxFractionDigits, String.format("O valor monetário deve ter no máximo %d casas decimais.", maxFractionDigits), fieldName);
    }
}
