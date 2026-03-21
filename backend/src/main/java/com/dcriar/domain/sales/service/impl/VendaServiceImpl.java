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
import com.dcriar.domain.product.repository.CanalVendaRepository;
import com.dcriar.domain.product.repository.MovimentacaoEstoqueProdutoRepository;
import com.dcriar.domain.product.repository.PrecoRepository;
import com.dcriar.domain.product.repository.ProdutoRepository;
import com.dcriar.domain.product.service.EstoqueProdutoService;
import com.dcriar.domain.sales.entity.ItemVenda;
import com.dcriar.domain.sales.entity.Venda;
import com.dcriar.domain.sales.entity.enums.TipoPrecoAplicado;
import com.dcriar.domain.sales.repository.VendaRepository;
import com.dcriar.domain.sales.service.VendaService;
import com.dcriar.exception.custom.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
        CanalVenda canalVenda = buscarCanalVenda(requestDTO.getCanalVendaId());
        List<ItemVenda> itemVendas = processarItensVenda(requestDTO.getItens(), canalVenda);

        Venda newVenda = Venda.from(canalVenda, itemVendas);
        newVenda.setValorTotal(calcularValorTotal(itemVendas));
        
        Venda savedVenda = vendaRepository.save(newVenda);
        return vendaMapper.toResponseDTO(savedVenda);
    }

    @Override
    @Transactional
    public VendaResponseDTO atualizarVenda(Long id, VendaRequestDTO requestDTO) {
        Venda vendaExistente = vendaRepository.findById(id)
                .orElseThrow(() -> new VendaNaoEncontradaException(id));

        // 1. Estorna o estoque da venda antiga
        performStockReversal(vendaExistente);

        // 2. Prepara os novos dados
        CanalVenda novoCanal = buscarCanalVenda(requestDTO.getCanalVendaId());
        List<ItemVenda> novosItens = processarItensVenda(requestDTO.getItens(), novoCanal);

        // 3. Atualiza a entidade existente (mantendo o ID)
        vendaExistente.updateFrom(novoCanal, novosItens);
        vendaExistente.setValorTotal(calcularValorTotal(novosItens));

        Venda savedVenda = vendaRepository.save(vendaExistente);
        return vendaMapper.toResponseDTO(savedVenda);
    }

    @Override
    @Transactional
    public void deletarVenda(Long id) {
        Venda venda = vendaRepository.findById(id)
                .orElseThrow(() -> new VendaNaoEncontradaException(id));

        performStockReversal(venda);
        vendaRepository.delete(venda);
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

    private CanalVenda buscarCanalVenda(Long id) {
        return canalVendaRepository.findById(id)
                .orElseThrow(() -> new CanalVendaNaoEncontradoException(id));
    }

    private BigDecimal calcularValorTotal(List<ItemVenda> itens) {
        return itens.stream()
                .map(ItemVenda::getPrecoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Processa a lista de itens da requisição de venda.
     * Realiza a busca em lote de produtos e preços para performance, valida a existência e preços,
     * calcula os valores unitários e totais, e executa a baixa de estoque.
     *
     * @param itensDTO Lista de DTOs dos itens da venda.
     * @param canalVenda Canal de venda para contexto de estoque.
     * @return Lista de entidades ItemVenda prontas para persistência.
     */
    private List<ItemVenda> processarItensVenda(List<ItemVendaRequestDTO> itensDTO, CanalVenda canalVenda) {
        Set<Long> produtoIds = itensDTO.stream()
                .map(ItemVendaRequestDTO::getProdutoId)
                .collect(Collectors.toSet());

        Map<Long, Produto> produtosMap = produtoRepository.findAllById(produtoIds).stream()
                .collect(Collectors.toMap(Produto::getId, Function.identity()));

        if (produtosMap.size() != produtoIds.size()) {
             produtoIds.removeAll(produtosMap.keySet());
             throw new ProdutoNaoEncontradoException(produtoIds.iterator().next());
        }

        List<Preco> precosList = precoRepository.findByProdutoIn(produtosMap.values());
        Map<Long, Preco> precosMap = precosList.stream()
                .collect(Collectors.toMap(p -> p.getProduto().getId(), Function.identity()));

        List<ItemVenda> itemVendas = new ArrayList<>();

        for (ItemVendaRequestDTO itemDTO : itensDTO) {
            Produto produto = produtosMap.get(itemDTO.getProdutoId());
            Preco preco = precosMap.get(itemDTO.getProdutoId());

            if (preco == null) {
                throw new PrecoComercialNaoDefinidoException(produto.getId());
            }

            BigDecimal precoComercialOriginal = normalizarValorMonetario(preco.getValor());
            TipoPrecoAplicado tipoPrecoAplicado = TipoPrecoAplicado.from(itemDTO.getTipoPrecoAplicado());
            BigDecimal unitPrice = resolverPrecoUnitario(itemDTO, precoComercialOriginal, tipoPrecoAplicado);
            BigDecimal itemTotalPrice = resolverPrecoTotal(itemDTO, unitPrice, tipoPrecoAplicado);

            // Realiza a baixa efetiva no estoque e registra a movimentação de saída.
            performStockReduction(produto, canalVenda, itemDTO.getQuantidade());

            itemVendas.add(ItemVenda.builder()
                    .produto(produto)
                    .quantidade(itemDTO.getQuantidade())
                    .precoComercialOriginal(precoComercialOriginal)
                    .precoUnitario(unitPrice)
                    .precoTotal(itemTotalPrice)
                    .tipoPrecoAplicado(tipoPrecoAplicado)
                    .motivoAlteracaoPreco(normalizarMotivo(itemDTO.getMotivoAlteracaoPreco()))
                    .build());
        }
        return itemVendas;
    }

    private BigDecimal resolverPrecoUnitario(
            ItemVendaRequestDTO itemDTO,
            BigDecimal precoComercialOriginal,
            TipoPrecoAplicado tipoPrecoAplicado
    ) {
        return switch (tipoPrecoAplicado) {
            case PRECO_PADRAO -> {
                BigDecimal precoAplicado = normalizarValorMonetario(itemDTO.getPrecoAplicado());
                if (precoAplicado.compareTo(precoComercialOriginal) != 0) {
                    throw PrecoVendaInvalidoException.precoPadraoDivergente(precoComercialOriginal, precoAplicado);
                }
                yield precoComercialOriginal;
            }
            case PRECO_ALTERADO -> normalizarValorUnitario(itemDTO.getPrecoAplicado());
            case DESCONTO_TOTAL -> calcularPrecoUnitarioPorTotal(itemDTO.getPrecoTotal(), itemDTO.getQuantidade());
        };
    }

    private BigDecimal resolverPrecoTotal(
            ItemVendaRequestDTO itemDTO,
            BigDecimal precoUnitario,
            TipoPrecoAplicado tipoPrecoAplicado
    ) {
        if (tipoPrecoAplicado == TipoPrecoAplicado.DESCONTO_TOTAL) {
            return normalizarValorMonetario(itemDTO.getPrecoTotal());
        }
        return precoUnitario.multiply(BigDecimal.valueOf(itemDTO.getQuantidade())).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calcularPrecoUnitarioPorTotal(BigDecimal precoTotalInformado, Integer quantidade) {
        BigDecimal total = normalizarValorMonetario(precoTotalInformado);
        return total.divide(BigDecimal.valueOf(quantidade), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizarValorMonetario(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizarValorUnitario(BigDecimal valor) {
        return valor.setScale(4, RoundingMode.HALF_UP);
    }

    private String normalizarMotivo(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            return null;
        }
        return motivo.trim();
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

    /**
     * Realiza o estorno do estoque para uma venda cancelada ou em edição.
     * Adiciona a quantidade dos itens de volta ao estoque e registra a movimentação de entrada.
     */
    private void performStockReversal(Venda venda) {
        for (ItemVenda item : venda.getItens()) {
            // 1. Registra movimentação de estorno para auditoria (AUMENTA O ESTOQUE FÍSICO)
            // IMPORTANTE: Deve ser feito ANTES de ajustar o canal para garantir que o teto físico suba primeiro.
            MovimentacaoEstoqueProdutoRequestDTO movimentacaoDTO = MovimentacaoEstoqueProdutoRequestDTO.builder()
                    .produtoId(item.getProduto().getId())
                    .tipo(TipoMovimentacaoProduto.ENTRADA_ESTORNO.name())
                    .quantidade(item.getQuantidade())
                    .motivo(String.format("Estorno de venda #%d", venda.getId()))
                    .build();
            
            MovimentacaoEstoqueProduto movimentacaoEstorno = MovimentacaoEstoqueProduto.from(movimentacaoDTO, item.getProduto());
            movimentacaoEstoqueProdutoRepository.save(movimentacaoEstorno);

            // 2. Estorna o estoque do canal (adiciona de volta, quantidade positiva)
            AjusteEstoqueRequestDTO ajusteDTO = AjusteEstoqueRequestDTO.builder()
                    .produtoId(item.getProduto().getId())
                    .canalVendaId(venda.getCanalVenda().getId())
                    .quantidade(item.getQuantidade())
                    .build();
            estoqueProdutoService.ajustarEstoque(ajusteDTO);
        }
    }
}
