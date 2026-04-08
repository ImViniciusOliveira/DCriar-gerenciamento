package com.dcriar.exception.custom;

import lombok.Getter;

/**
 * Exceção lançada ao tentar criar ou atualizar um {@link com.dcriar.domain.product.entity.Produto}
 * com um nome que já existe no banco de dados.
 */
@Getter
public class ProdutoNomeDuplicadoException extends RuntimeException {

    private final String nome;

    public ProdutoNomeDuplicadoException(String nome) {
        super(String.format(
                "Já existe um produto cadastrado com o nome '%s'. Use um nome diferente para continuar.",
                nome
        ));
        this.nome = nome;
    }
}
