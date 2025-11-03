package com.dcriar.exception.custom;

/**
 * Exceção lançada quando uma operação (como simulação ou produção) não encontra lotes de matéria-prima com estoque disponível.
 */
public class NenhumLoteComEstoqueException extends RuntimeException {

    public NenhumLoteComEstoqueException(String message) {
        super(message);
    }
}
