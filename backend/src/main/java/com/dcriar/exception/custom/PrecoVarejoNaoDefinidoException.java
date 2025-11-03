package com.dcriar.exception.custom;

import lombok.Getter;

/**
 * Exceção lançada ao tentar realizar uma operação que exige um preço de varejo
 * para um produto que não o possui definido.
 * <p>
 * Esta é uma violação de regra de negócio que impede, por exemplo, que um produto
 * seja associado a um canal de venda de varejo sem um preço configurado.
 */
@Getter
public class PrecoVarejoNaoDefinidoException extends RuntimeException {

    /**
     * O ID do produto que está sem preço de varejo.
     */
    private final Long produtoId;

    /**
     * Constrói a exceção com o ID do produto problemático.
     *
     * @param produtoId O ID do produto que não possui preço de varejo.
     */
    public PrecoVarejoNaoDefinidoException(Long produtoId) {
        super(String.format("Preço de varejo não definido para o produto ID: %d", produtoId));
        this.produtoId = produtoId;
    }
}
