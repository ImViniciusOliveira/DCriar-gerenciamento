package com.dcriar.exception.custom;

import lombok.Getter;

/**
 * Exceção lançada quando uma operação depende do preço comercial de um produto
 * e esse preço ainda não foi configurado.
 */
@Getter
public class PrecoComercialNaoDefinidoException extends RuntimeException {

    private final Long produtoId;

    public PrecoComercialNaoDefinidoException(Long produtoId) {
        super(String.format("Preço comercial não definido para o produto ID: %d", produtoId));
        this.produtoId = produtoId;
    }
}
