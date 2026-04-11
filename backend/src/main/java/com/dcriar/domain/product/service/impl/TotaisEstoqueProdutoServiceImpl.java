package com.dcriar.domain.product.service.impl;

import com.dcriar.domain.product.model.TotaisEstoqueProduto;
import com.dcriar.domain.product.repository.EstoqueRepository;
import com.dcriar.domain.product.repository.MovimentacaoEstoqueProdutoRepository;
import com.dcriar.domain.product.service.TotaisEstoqueProdutoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementação que calcula os totais reais do estoque de produto
 * diretamente das movimentações físicas e dos estoques distribuídos por canal.
 */
@Service
@RequiredArgsConstructor
public class TotaisEstoqueProdutoServiceImpl implements TotaisEstoqueProdutoService {

    private final MovimentacaoEstoqueProdutoRepository movimentacaoEstoqueProdutoRepository;
    private final EstoqueRepository estoqueRepository;

    @Override
    @Transactional(readOnly = true)
    public TotaisEstoqueProduto obterTotais(Long produtoId) {
        int estoqueFisicoTotal = movimentacaoEstoqueProdutoRepository.sumQuantidadeByProdutoId(produtoId);
        int estoqueDistribuidoTotal = estoqueRepository.sumQuantidadeByProdutoId(produtoId);

        return new TotaisEstoqueProduto(
                estoqueFisicoTotal,
                estoqueDistribuidoTotal,
                estoqueFisicoTotal - estoqueDistribuidoTotal
        );
    }
}
