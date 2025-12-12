package com.dcriar.domain.sales.service.impl;

import com.dcriar.api.dto.request.product.AjusteEstoqueRequestDTO;
import com.dcriar.api.dto.request.product.MovimentacaoEstoqueProdutoRequestDTO;
import com.dcriar.api.dto.request.sales.ItemVendaRequestDTO;
import com.dcriar.api.dto.request.sales.VendaRequestDTO;
import com.dcriar.api.dto.response.sales.VendaResponseDTO;
import com.dcriar.api.mapper.sales.VendaMapper;
import com.dcriar.domain.product.entity.CanalVenda;
import com.dcriar.domain.product.entity.MovimentacaoEstoqueProduto;
import com.dcriar.domain.product.entity.Preco;
import com.dcriar.domain.product.entity.Produto;
import com.dcriar.domain.product.entity.enums.TipoMovimentacaoProduto;
import com.dcriar.domain.product.entity.enums.TipoPreco;
import com.dcriar.domain.product.repository.CanalVendaRepository;
import com.dcriar.domain.product.repository.MovimentacaoEstoqueProdutoRepository;
import com.dcriar.domain.product.repository.PrecoRepository;
import com.dcriar.domain.product.repository.ProdutoRepository;
import com.dcriar.domain.product.service.EstoqueProdutoService;
import com.dcriar.domain.sales.entity.ItemVenda;
import com.dcriar.domain.sales.entity.Venda;
import com.dcriar.domain.sales.repository.VendaRepository;
import com.dcriar.domain.sales.service.VendaService;
import com.dcriar.exception.custom.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VendaServiceImpl implements VendaService {

    private final VendaRepository vendaRepository;
    private final ProdutoRepository produtoRepository;
    private final CanalVendaRepository canalVendaRepository;
    private final PrecoRepository precoRepository;
    private final MovimentacaoEstoqueProdutoRepository movimentacaoEstoqueProdutoRepository;
    private final EstoqueProdutoService estoqueProdutoService;
    private final VendaMapper vendaMapper;

    @Override
    @Transactional
    public VendaResponseDTO registrarVenda(VendaRequestDTO requestDTO) {
        // 1. Valida Canal (1 Query)
        CanalVenda canalVenda = canalVendaRepository.findById(requestDTO.getCanalVendaId())
                .orElseThrow(() -> new CanalVendaNaoEncontradoException(requestDTO.getCanalVendaId()));

        // --- OTIMIZAÇÃO BATCH (Alta Performance) ---
        
        // A. Coleta IDs
        Set<Long> produtoIds = requestDTO.getItens().stream()
                .map(ItemVendaRequestDTO::getProdutoId)
                .collect(Collectors.toSet());

        // B. Busca Produtos em Lote (1 Query IN)
        Map<Long, Produto> produtosMap = produtoRepository.findAllById(produtoIds).stream()
                .collect(Collectors.toMap(Produto::getId, Function.identity()));

        if (produtosMap.size() != produtoIds.size()) {
             produtoIds.removeAll(produtosMap.keySet());
             throw new ProdutoNaoEncontradoException(produtoIds.iterator().next());
        }

        // C. Busca Preços em Lote (1 Query IN)
        List<Preco> precosList = precoRepository.findByProdutoInAndTipoPreco(produtosMap.values(), TipoPreco.VAREJO);
        Map<Long, Preco> precosMap = precosList.stream()
                .collect(Collectors.toMap(p -> p.getProduto().getId(), Function.identity()));

        // -------------------------------------------

        List<ItemVenda> itemVendas = new ArrayList<>();

        // Loop em Memória (Zero queries de leitura aqui dentro)
        for (ItemVendaRequestDTO itemDTO : requestDTO.getItens()) {
            Produto produto = produtosMap.get(itemDTO.getProdutoId());
            Preco preco = precosMap.get(itemDTO.getProdutoId());

            if (preco == null) {
                throw new PrecoVarejoNaoDefinidoException(produto.getId());
            }

            BigDecimal unitPrice = preco.isPromocaoAtiva() && preco.getValorPromocional() != null
                    ? preco.getValorPromocional()
                    : preco.getValor();

            BigDecimal itemTotalPrice = unitPrice.multiply(BigDecimal.valueOf(itemDTO.getQuantidade()));

            // A baixa de estoque gera escrita (necessário)
            performStockReduction(produto, canalVenda, itemDTO.getQuantidade());

            itemVendas.add(ItemVenda.builder()
                    .produto(produto)
                    .quantidade(itemDTO.getQuantidade())
                    .precoUnitario(unitPrice)
                    .precoTotal(itemTotalPrice)
                    .build());
        }

        Venda newVenda = Venda.from(canalVenda, itemVendas);
        BigDecimal totalAmount = itemVendas.stream()
                .map(ItemVenda::getPrecoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        newVenda.setValorTotal(totalAmount);
        
        Venda savedVenda = vendaRepository.save(newVenda);
        return vendaMapper.toResponseDTO(savedVenda);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VendaResponseDTO> findAll(Pageable pageable) {
        return vendaRepository.findAll(pageable)
                .map(vendaMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public VendaResponseDTO findById(Long id) {
        return vendaRepository.findById(id).map(vendaMapper::toResponseDTO).orElseThrow(() -> new VendaNaoEncontradaException(id));
    }

    private void performStockReduction(Produto produto, CanalVenda canalVenda, int quantity) {
        AjusteEstoqueRequestDTO ajusteDTO = AjusteEstoqueRequestDTO.builder()
                .produtoId(produto.getId())
                .canalVendaId(canalVenda.getId())
                .quantidade(quantity * -1)
                .build();
        estoqueProdutoService.ajustarEstoque(ajusteDTO);

        MovimentacaoEstoqueProdutoRequestDTO movimentacaoDTO = MovimentacaoEstoqueProdutoRequestDTO.builder()
                .produtoId(produto.getId())
                .tipo(TipoMovimentacaoProduto.SAIDA_VENDA.name())
                .quantidade(quantity * -1)
                .motivo(String.format("Venda no canal: %s", canalVenda.getNome()))
                .build();
        MovimentacaoEstoqueProduto movimentacaoVenda = MovimentacaoEstoqueProduto.from(movimentacaoDTO, produto);
        movimentacaoEstoqueProdutoRepository.save(movimentacaoVenda);
    }
}
