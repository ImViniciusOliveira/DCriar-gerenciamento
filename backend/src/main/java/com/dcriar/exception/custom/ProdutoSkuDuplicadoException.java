package com.dcriar.exception.custom;

import lombok.Getter;

/**
 * Exceção lançada ao tentar criar ou atualizar um {@link com.dcriar.domain.product.entity.Produto}
 * com um SKU que já existe no banco de dados.
 */
@Getter
public class ProdutoSkuDuplicadoException extends RuntimeException {

    private final String sku;

    public ProdutoSkuDuplicadoException(String sku) {
        super(String.format("Já existe um produto com o SKU: %s", sku));
        this.sku = sku;
    }
}
