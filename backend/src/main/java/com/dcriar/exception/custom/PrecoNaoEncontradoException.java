package com.dcriar.exception.custom;

import lombok.Getter;

/**
 * Exceção lançada quando uma operação tenta acessar um {@link com.dcriar.domain.product.entity.Preco}
 * que não existe no banco de dados, usando seu ID.
 */
@Getter
public class PrecoNaoEncontradoException extends RuntimeException {

    private final Long id;

    public PrecoNaoEncontradoException(Long id) {
        super(String.format("Preço #%d não foi encontrado. Verifique o identificador informado e tente novamente.", id));
        this.id = id;
    }
}
