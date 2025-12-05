package com.dcriar.exception.custom;

public class LoteEmUsoException extends RegraNegocioException {
    public LoteEmUsoException(Long id) {
        super(String.format("O lote de matéria-prima com ID %d não pode ser excluído pois já possui movimentações.", id));
    }
}
