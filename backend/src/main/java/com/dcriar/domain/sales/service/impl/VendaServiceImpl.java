package com.dcriar.domain.sales.service.impl;

import com.dcriar.api.dto.request.product.AjusteEstoqueRequestDTO;
import com.dcriar.api.dto.request.product.MovimentacaoEstoqueProdutoRequestDTO;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementação da lógica de negócio para o módulo de Vendas.
 * <p>
 * Esta classe orquestra o processo de registro de vendas, incluindo a validação de dados,
 * cálculo de preços, a complexa baixa de estoque em duas fases (canal e mestre)
 * e a persistência dos dados de forma transacional.
 */
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

    /**
     * Registra uma nova venda no sistema e orquestra a baixa automática de estoque.
     * <p>
     * <b>Processo de Orquestração e Regras de Negócio:</b>
     * <ol>
     *     <li><b>Validação do Canal:</b> O canal de venda especificado deve existir.</li>
     *     <li><b>Processamento de Itens:</b> Para cada item na venda:
     *         <ul>
     *             <li>O produto deve existir.</li>
     *             <li><b>Lógica de Preço:</b> O preço é determinado com base no {@link TipoPreco#VAREJO}. Se uma promoção estiver ativa ({@code promocaoAtiva=true}), o {@code valorPromocional} tem precedência sobre o {@code valor} normal.</li>
     *             <li><b>Baixa de Estoque:</b> O estoque é reduzido em duas fases (ver {@link #performStockReduction(Produto, CanalVenda, int)}). A validação de saldo ocorre na baixa do estoque do canal.</li>
     *         </ul>
     *     </li>
     *     <li><b>Transacionalidade:</b> A operação é atômica. Se o estoque de qualquer item for insuficiente, a venda inteira é revertida (rollback), garantindo a consistência dos dados.</li>
     * </ol>
     *
     * @param requestDTO O DTO contendo os dados da nova venda.
     * @return Um {@link VendaResponseDTO} representando a venda registrada.
     * @throws CanalVendaNaoEncontradoException se o canal de venda não for encontrado.
     * @throws ProdutoNaoEncontradoException se um produto de um item não for encontrado.
     * @throws PrecoVarejoNaoDefinidoException se o preço de varejo para um produto não estiver definido.
     * @throws EstoqueInsuficienteCanalException se não houver estoque suficiente no canal para um produto (propagada pelo {@code EstoqueProdutoService}).
     */
    @Override
    @Transactional
    public VendaResponseDTO registrarVenda(VendaRequestDTO requestDTO) {
        // 1. Valida e busca o canal de venda.
        CanalVenda canalVenda = canalVendaRepository.findById(requestDTO.getCanalVendaId())
                .orElseThrow(() -> new CanalVendaNaoEncontradoException(requestDTO.getCanalVendaId()));

        // 2. Processa cada item da venda para criar as entidades ItemVenda.
        List<ItemVenda> itemVendas = requestDTO.getItens().stream().map(itemDTO -> {
            Produto produto = produtoRepository.findById(itemDTO.getProdutoId())
                    .orElseThrow(() -> new ProdutoNaoEncontradoException(itemDTO.getProdutoId()));

            // Regra de negócio: A venda sempre utiliza o preço de VAREJO.
            Preco preco = precoRepository.findByProdutoAndTipoPreco(produto, TipoPreco.VAREJO)
                    .orElseThrow(() -> new PrecoVarejoNaoDefinidoException(produto.getId()));

            // Regra de negócio: Aplica o preço promocional se a promoção estiver ativa.
            BigDecimal unitPrice = preco.isPromocaoAtiva() && preco.getValorPromocional() != null
                    ? preco.getValorPromocional()
                    : preco.getValor();

            BigDecimal itemTotalPrice = unitPrice.multiply(BigDecimal.valueOf(itemDTO.getQuantidade()));

            // Orquestra a baixa de estoque (validação no canal + registro no mestre).
            performStockReduction(produto, canalVenda, itemDTO.getQuantidade());

            return ItemVenda.builder()
                    .produto(produto)
                    .quantidade(itemDTO.getQuantidade())
                    .precoUnitario(unitPrice)
                    .precoTotal(itemTotalPrice)
                    .build();
        }).collect(Collectors.toList());

        // 3. Cria a entidade Venda, calcula o total e a persiste.
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
    public List<VendaResponseDTO> findAll() {
        return vendaRepository.findAll().stream()
                .map(vendaMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public VendaResponseDTO findById(Long id) {
        return vendaRepository.findById(id)
                .map(vendaMapper::toResponseDTO)
                .orElseThrow(() -> new VendaNaoEncontradaException(id));
    }

    /**
     * Orquestra a baixa de estoque em duas fases: validação/redução no canal e registro no histórico mestre.
     * <p>
     * <b>Processo de Baixa de Estoque:</b>
     * <ol>
     *     <li><b>Fase 1: Baixa no Canal de Venda.</b> Invoca o {@link EstoqueProdutoService} para reduzir o estoque no canal.
     *     Esta é a etapa de validação crítica: se o estoque do canal for insuficiente, uma {@link EstoqueInsuficienteCanalException}
     *     será lançada, e a transação da venda será revertida.</li>
     *     <li><b>Fase 2: Registro no Estoque Mestre.</b> Se a baixa no canal for bem-sucedida, uma {@link MovimentacaoEstoqueProduto}
     *     de saída é criada para registrar a transação no histórico geral do produto (livro-razão), garantindo a rastreabilidade.</li>
     * </ol>
     *
     * @param produto O produto que terá seu estoque reduzido.
     * @param canalVenda O canal de venda onde a baixa será efetuada.
     * @param quantity A quantidade a ser removida (valor positivo que será convertido para negativo).
     * @throws EstoqueInsuficienteCanalException se não houver estoque suficiente no canal.
     */
    private void performStockReduction(Produto produto, CanalVenda canalVenda, int quantity) {
        // Fase 1: Ajusta (e valida) o estoque no canal de venda.
        AjusteEstoqueRequestDTO ajusteDTO = AjusteEstoqueRequestDTO.builder()
                .produtoId(produto.getId())
                .canalVendaId(canalVenda.getId())
                .quantidade(quantity * -1) // Quantidade negativa para indicar saída.
                .build();
        estoqueProdutoService.ajustarEstoque(ajusteDTO);

        // Fase 2: Registra a movimentação no histórico mestre para fins de auditoria.
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
