package com.dcriar.domain.product.entity.enums;

/**
 * Enum que representa os diferentes tipos de precificação para um produto.
 * <p>
 * Usar um Enum garante a consistência dos dados e torna o código mais legível
 * e seguro contra erros de digitação.
 */
public enum TipoPreco {

    /**
     * Preço padrão para o consumidor final.
     */
    VAREJO,

    /**
     * Preço especial para revendedores ou compras em grande quantidade.
     */
    REVENDA
}
