package com.dcriar.api.validation.validator;

import com.dcriar.api.dto.request.production.OrdemDeCorteRequestDTO;
import com.dcriar.api.validation.annotation.ValidOrdemDeCorteRequest;
import com.dcriar.domain.production.enums.ModoCalculo;

import java.math.BigDecimal;

public class OrdemDeCorteRequestValidator extends BaseValidator<ValidOrdemDeCorteRequest, OrdemDeCorteRequestDTO> {

    @Override
    protected void validate(OrdemDeCorteRequestDTO dto) {
        addViolationIf(dto.getProdutoId() == null, "O ID do produto é obrigatório.", "produtoId");
        addViolationIf(dto.getLoteId() == null, "O ID do lote de matéria-prima é obrigatório.", "loteId");
        addViolationIf(dto.getQuantidadeProduzida() == null, "A quantidade de unidades do produto a serem produzidas é obrigatória.", "quantidadeProduzida");
        if (dto.getQuantidadeProduzida() != null) {
            addViolationIf(dto.getQuantidadeProduzida() <= 0, "A quantidade produzida deve ser um número positivo.", "quantidadeProduzida");
        }
        addViolationIf(dto.getModoCalculo() == null, "O modo de cálculo para o corte é obrigatório.", "modoCalculo");

        if (dto.getModoCalculo() == ModoCalculo.MANUAL) {
            addViolationIf(dto.getLarguraBlocoProdutosCm() == null, "Para o modo de cálculo MANUAL, o campo 'larguraBlocoProdutosCm' é obrigatório.", "larguraBlocoProdutosCm");
            addViolationIf(dto.getComprimentoBlocoProdutosCm() == null, "Para o modo de cálculo MANUAL, o campo 'comprimentoBlocoProdutosCm' é obrigatório.", "comprimentoBlocoProdutosCm");
            if (dto.getLarguraBlocoProdutosCm() != null) {
                addViolationIf(dto.getLarguraBlocoProdutosCm().compareTo(BigDecimal.ZERO) <= 0, "A largura do bloco de produtos deve ser um número positivo.", "larguraBlocoProdutosCm");
            }
            if (dto.getComprimentoBlocoProdutosCm() != null) {
                addViolationIf(dto.getComprimentoBlocoProdutosCm().compareTo(BigDecimal.ZERO) <= 0, "O comprimento do bloco de produtos deve ser um número positivo.", "comprimentoBlocoProdutosCm");
            }
        }
    }
}
