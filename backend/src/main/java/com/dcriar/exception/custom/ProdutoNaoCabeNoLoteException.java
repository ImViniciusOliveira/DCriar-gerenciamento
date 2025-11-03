package com.dcriar.exception.custom;

import lombok.Getter;

/**
 * Exceção lançada quando um produto não pode ser cortado de um lote de matéria-prima
 * porque suas dimensões excedem as dimensões úteis do lote.
 */
@Getter
public class ProdutoNaoCabeNoLoteException extends RuntimeException {

    /**
     * Constrói a exceção com a mensagem de erro.
     *
     * @param message A mensagem explicando por que o produto não cabe no lote.
     */
    public ProdutoNaoCabeNoLoteException(String message) {
        super(message);
    }
}
