package com.dcriar.exception.custom;

import lombok.Getter;

/**
 * Exceção lançada quando uma tentativa de excluir uma Ordem de Produção é bloqueada
 * por regras de integridade (ex: produtos já vendidos ou retalhos já utilizados).
 * <p>
 * Esta exceção garante que o sistema não permita operações que deixariam o estoque
 * em estado inconsistente ou negativo.
 */
@Getter
public class ImpossivelExcluirProducaoException extends RuntimeException {

    /**
     * Constrói a exceção com a mensagem detalhando o motivo do bloqueio.
     *
     * @param mensagem A descrição do erro e o motivo pelo qual a exclusão não é permitida.
     */
    public ImpossivelExcluirProducaoException(String mensagem) {
        super(mensagem);
    }
}
