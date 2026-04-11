package com.dcriar.domain.product.service;

import com.dcriar.domain.product.model.TotaisEstoqueProduto;

/**
 * Serviço responsável por calcular os totais reais de estoque de um produto
 * diretamente das fontes de verdade do banco.
 */
public interface TotaisEstoqueProdutoService {

    TotaisEstoqueProduto obterTotais(Long produtoId);
}
