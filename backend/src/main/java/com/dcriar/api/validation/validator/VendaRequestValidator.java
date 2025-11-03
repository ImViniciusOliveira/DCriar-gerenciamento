package com.dcriar.api.validation.validator;

import com.dcriar.api.dto.request.sales.ItemVendaRequestDTO;
import com.dcriar.api.dto.request.sales.VendaRequestDTO;
import com.dcriar.api.validation.annotation.ValidVendaRequest;

import java.util.List;

/**
 * Validador para o DTO {@link VendaRequestDTO}, acionado pela anotação {@link ValidVendaRequest}.
 * <p>
 * Este validador verifica se os campos essenciais da requisição de venda estão presentes:
 * <ul>
 *     <li>O {@code canalVendaId} não pode ser nulo.</li>
 *     <li>A lista de {@code items} não pode ser nula nem vazia.</li>
 * </ul>
 * A validação de cada item individual na lista é delegada para suas respectivas anotações.
 */
public class VendaRequestValidator extends BaseValidator<ValidVendaRequest, VendaRequestDTO> {

    @Override
    protected void validate(VendaRequestDTO dto) {
        addViolationIf(dto.getCanalVendaId() == null, "O ID do canal de venda é obrigatório.", "canalVendaId");

        List<ItemVendaRequestDTO> items = dto.getItems();
        addViolationIf(items == null || items.isEmpty(), "A lista de itens não pode estar vazia.", "items");
    }
}
