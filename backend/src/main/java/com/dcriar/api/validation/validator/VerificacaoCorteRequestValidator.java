package com.dcriar.api.validation.validator;

import com.dcriar.api.dto.request.production.VerificacaoCorteRequestDTO;
import com.dcriar.api.validation.annotation.ValidVerificacaoCorteRequest;
import com.dcriar.domain.production.enums.ModoCalculo;

import java.math.BigDecimal;

public class VerificacaoCorteRequestValidator extends BaseValidator<ValidVerificacaoCorteRequest, VerificacaoCorteRequestDTO> {

    @Override
    protected void validate(VerificacaoCorteRequestDTO dto) {
        addViolationIf(dto.getProdutoId() == null, "O ID do produto é obrigatório.", "produtoId");
        addViolationIf(dto.getLoteId() == null, "O ID do lote de matéria-prima é obrigatório.", "loteId");
        addViolationIf(dto.getQuantidade() == null, "A quantidade de unidades é obrigatória.", "quantidade");
        if (dto.getQuantidade() != null) {
            addViolationIf(dto.getQuantidade() <= 0, "A quantidade deve ser um número positivo.", "quantidade");
        }
        addViolationIf(dto.getModoCalculo() == null, "O modo de cálculo é obrigatório.", "modoCalculo");

        if (dto.getModoCalculo() == ModoCalculo.MANUAL) {
            addViolationIf(dto.getLarguraFinalCm() == null, "Para o modo MANUAL, a largura final é obrigatória.", "larguraFinalCm");
            addViolationIf(dto.getComprimentoFinalCm() == null, "Para o modo MANUAL, o comprimento final é obrigatório.", "comprimentoFinalCm");
            if (dto.getLarguraFinalCm() != null) {
                addViolationIf(dto.getLarguraFinalCm().compareTo(BigDecimal.ZERO) <= 0, "A largura final deve ser positiva.", "larguraFinalCm");
            }
            if (dto.getComprimentoFinalCm() != null) {
                addViolationIf(dto.getComprimentoFinalCm().compareTo(BigDecimal.ZERO) <= 0, "O comprimento final deve ser positivo.", "comprimentoFinalCm");
            }
        }
    }
}
