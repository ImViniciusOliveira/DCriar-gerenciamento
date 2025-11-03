package com.dcriar.api.validation.validator;

import com.dcriar.api.dto.request.product.PrecoRequestDTO;
import com.dcriar.api.validation.annotation.ValidPrecoRequest;

import java.math.BigDecimal;

public class PrecoRequestValidator extends BaseValidator<ValidPrecoRequest, PrecoRequestDTO> {

    @Override
    protected void validate(PrecoRequestDTO dto) {
        addViolationIf(dto.getProdutoId() == null, "O ID do produto é obrigatório.", "produtoId");
        addViolationIf(dto.getTipoPreco() == null || dto.getTipoPreco().isBlank(), "O tipo de preço é obrigatório.", "tipoPreco");
        addViolationIf(dto.getValor() == null, "O valor é obrigatório.", "valor");
        if (dto.getValor() != null) {
            addViolationIf(dto.getValor().compareTo(BigDecimal.ZERO) < 0, "O valor não pode ser negativo.", "valor");
        }
        if (dto.getValorPromocional() != null) {
            addViolationIf(dto.getValorPromocional().compareTo(BigDecimal.ZERO) < 0, "O valor promocional não pode ser negativo.", "valorPromocional");
        }
        addViolationIf(dto.getPromocaoAtiva() == null, "O campo 'promoção ativa' é obrigatório.", "promocaoAtiva");
    }
}
