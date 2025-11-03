package com.dcriar.api.validation.validator;

import com.dcriar.api.dto.request.production.OrdemDeProducaoRequestDTO;
import com.dcriar.api.validation.annotation.ValidOrdemDeProducaoRequest;

import java.math.BigDecimal;

public class OrdemDeProducaoRequestValidator extends BaseValidator<ValidOrdemDeProducaoRequest, OrdemDeProducaoRequestDTO> {

    @Override
    protected void validate(OrdemDeProducaoRequestDTO dto) {
        addViolationIf(dto.getProdutoId() == null, "O ID do produto é obrigatório.", "produtoId");
        addViolationIf(dto.getLotesConsumidosIds() == null || dto.getLotesConsumidosIds().isEmpty(), "A lista de IDs de lotes consumidos é obrigatória.", "lotesConsumidosIds");
        addViolationIf(dto.getQuantidadeProduzida() == null, "A quantidade produzida é obrigatória.", "quantidadeProduzida");
        if (dto.getQuantidadeProduzida() != null) {
            addViolationIf(dto.getQuantidadeProduzida() <= 0, "A quantidade produzida deve ser um número positivo.", "quantidadeProduzida");
        }
        addViolationIf(dto.getModoCalculo() == null || dto.getModoCalculo().isBlank(), "O modo de cálculo é obrigatório.", "modoCalculo");

        if (dto.getLarguraFinalCm() != null) {
            addViolationIf(dto.getLarguraFinalCm().compareTo(BigDecimal.ZERO) <= 0, "A largura final deve ser um número positivo.", "larguraFinalCm");
        }

        if (dto.getComprimentoFinalCm() != null) {
            addViolationIf(dto.getComprimentoFinalCm().compareTo(BigDecimal.ZERO) <= 0, "O comprimento final deve ser um número positivo.", "comprimentoFinalCm");
        }

        if (dto.getMotivo() != null) {
            addViolationIf(dto.getMotivo().length() > 255, "O motivo deve ter no máximo 255 caracteres.", "motivo");
        }
    }
}
