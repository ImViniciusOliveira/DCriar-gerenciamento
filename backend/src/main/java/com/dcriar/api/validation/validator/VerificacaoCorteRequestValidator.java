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
            addViolationIf(dto.getLarguraBlocoProdutosCm() == null, "Para o modo MANUAL, a largura do bloco de produtos é obrigatória.", "larguraBlocoProdutosCm");
            addViolationIf(dto.getComprimentoBlocoProdutosCm() == null, "Para o modo MANUAL, o comprimento do bloco de produtos é obrigatório.", "comprimentoBlocoProdutosCm");
            if (dto.getLarguraBlocoProdutosCm() != null) {
                addViolationIf(dto.getLarguraBlocoProdutosCm().compareTo(BigDecimal.ZERO) <= 0, "A largura do bloco de produtos deve ser positiva.", "larguraBlocoProdutosCm");
            }
            if (dto.getComprimentoBlocoProdutosCm() != null) {
                addViolationIf(dto.getComprimentoBlocoProdutosCm().compareTo(BigDecimal.ZERO) <= 0, "O comprimento do bloco de produtos deve ser positivo.", "comprimentoBlocoProdutosCm");
            }
        }
    }
}
