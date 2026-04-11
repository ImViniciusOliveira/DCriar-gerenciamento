package com.dcriar.domain.product.model;

/**
 * Snapshot consistente dos totais de estoque de um produto.
 * <p>
 * Deve ser usado em regras de negócio sensíveis a estado transacional,
 * evitando depender de campos derivados via {@code @Formula}.
 */
public record TotaisEstoqueProduto(
        int estoqueFisicoTotal,
        int estoqueDistribuidoTotal,
        int estoqueDisponivelParaAlocar
) {
}
