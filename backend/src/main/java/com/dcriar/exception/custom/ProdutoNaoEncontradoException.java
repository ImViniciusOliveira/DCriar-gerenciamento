package com.dcriar.exception.custom;

import lombok.Getter;

/**
 * Exceção lançada quando uma operação tenta acessar um {@link com.dcriar.domain.product.entity.Produto}
 * que não existe no banco de dados, usando seu ID.
 * <p>
 * É tipicamente capturada por um handler global que retorna uma resposta
 * HTTP 404 (Not Found).
 */
@Getter
public class ProdutoNaoEncontradoException extends RuntimeException {

    /**
     * O ID do produto que não foi encontrado.
     */
    private final Long id;

    /**
     * Constrói a exceção com o ID do produto não encontrado.
     *
     * @param id O ID utilizado na busca que falhou.
     */
    public ProdutoNaoEncontradoException(Long id) {
        super(String.format("Produto não encontrado com o ID: %d", id));
        this.id = id;
    }
}
