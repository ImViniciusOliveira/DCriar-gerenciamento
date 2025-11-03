package com.dcriar.api.validation.validator;

import com.dcriar.api.dto.request.product.CanalVendaRequestDTO;
import com.dcriar.api.validation.annotation.ValidCanalVendaRequest;

public class CanalVendaRequestValidator extends BaseValidator<ValidCanalVendaRequest, CanalVendaRequestDTO> {

    @Override
    protected void validate(CanalVendaRequestDTO dto) {
        addViolationIf(dto.getNome() == null || dto.getNome().isBlank(), "O nome do canal de venda é obrigatório.", "nome");
        if (dto.getNome() != null) {
            addViolationIf(dto.getNome().length() > 50, "O nome do canal de venda deve ter no máximo 50 caracteres.", "nome");
        }
    }
}
