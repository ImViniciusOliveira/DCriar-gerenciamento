package com.dcriar.domain.product.service.impl;

import com.dcriar.api.dto.request.product.AjusteEstoqueProdutoRequestDTO;
import com.dcriar.api.dto.request.product.AjusteEstoqueRequestDTO;
import com.dcriar.api.dto.request.product.EstoqueRequestDTO;
import com.dcriar.api.dto.request.product.MovimentacaoEstoqueProdutoRequestDTO;
import com.dcriar.api.dto.response.product.EstoqueResponseDTO;
import com.dcriar.api.dto.response.product.MovimentacaoProdutoResponseDTO;
import com.dcriar.api.dto.response.product.ProdutoEstoqueResponseDTO;
import com.dcriar.api.mapper.product.EstoqueMapper;
import com.dcriar.api.mapper.product.MovimentacaoProdutoMapper;
import com.dcriar.api.mapper.product.ProdutoEstoqueDTOMapper;
import com.dcriar.domain.product.entity.CanalVenda;
import com.dcriar.domain.product.entity.Estoque;
import com.dcriar.domain.product.entity.MovimentacaoEstoqueProduto;
import com.dcriar.domain.product.entity.Produto;
import com.dcriar.domain.product.entity.enums.TipoMovimentacaoProduto;
import com.dcriar.domain.product.repository.CanalVendaRepository;
import com.dcriar.domain.product.repository.EstoqueRepository;
import com.dcriar.domain.product.repository.MovimentacaoEstoqueProdutoRepository;
import com.dcriar.domain.product.repository.ProdutoRepository;
import com.dcriar.domain.product.service.EstoqueProdutoService;
import com.dcriar.exception.custom.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

/**
 * Implementação da lógica de negócio para o gerenciamento de estoque de produtos acabados.
 * Esta classe orquestra as operações de ajuste de estoque nos canais de venda e no estoque físico (mestre),
 * garantindo a consistência e a aplicação das regras de negócio.
 */
@Service
@RequiredArgsConstructor
public class EstoqueProdutoServiceImpl implements EstoqueProdutoService {

    private final EstoqueRepository estoqueRepository;
    private final ProdutoRepository produtoRepository;
    private final CanalVendaRepository canalVendaRepository;
    private final EstoqueMapper estoqueMapper;
    private final MovimentacaoEstoqueProdutoRepository movimentacaoEstoqueProdutoRepository;
    private final MovimentacaoProdutoMapper movimentacaoProdutoMapper;
    private final ProdutoEstoqueDTOMapper produtoEstoqueDTOMapper;

    /**
     * Ajusta o estoque de um produto acabado em um canal de venda específico (distribuição).
     * Esta operação pode aumentar ou diminuir o estoque de um produto em um canal.
     * <p>
     * <b>Regras de negócio aplicadas:</b>
     * <ul>
     *     <li>Se não existir um registro de estoque para a combinação produto/canal, um novo será criado com quantidade zero antes do ajuste.</li>
     *     <li>Ao adicionar estoque a um canal, o total distribuído não pode ultrapassar o estoque físico total disponível.</li>
     *     <li>O estoque de um canal não pode se tornar negativo após a operação.</li>
     * </ul>
     *
     * @param requestDTO O DTO contendo os dados do ajuste de estoque (produtoId, canalVendaId, quantidade).
     * @return Um {@link EstoqueResponseDTO} representando o estado atualizado do estoque no canal.
     * @throws ProdutoNaoEncontradoException se o produto especificado não for encontrado.
     * @throws CanalVendaNaoEncontradoException se o canal de venda especificado não for encontrado.
     * @throws AlocacaoEstoqueExcedeTotalException se o total distribuído exceder o estoque físico total.
     * @throws EstoqueInsuficienteCanalException se a operação resultar em estoque negativo no canal.
     */
    @Override
    @Transactional
    public EstoqueResponseDTO ajustarEstoque(AjusteEstoqueRequestDTO requestDTO) {
        Produto produto = findProdutoById(requestDTO.getProdutoId());
        CanalVenda canalVenda = findCanalVendaById(requestDTO.getCanalVendaId());

        // 1. Validação de regra de negócio: ao adicionar estoque em um canal, o total distribuído
        // não pode ultrapassar o estoque físico disponível.
        if (requestDTO.getQuantidade() > 0) {
            Integer estoqueFisicoTotal = movimentacaoEstoqueProdutoRepository.findSaldoByProduto(produto);
            int totalDistribuido = estoqueRepository.findAllByProduto(produto).stream()
                    .mapToInt(Estoque::getQuantidade)
                    .sum();

            int novoTotalDistribuido = totalDistribuido + requestDTO.getQuantidade();

            if (novoTotalDistribuido > estoqueFisicoTotal) {
                throw new AlocacaoEstoqueExcedeTotalException(
                        requestDTO.getQuantidade(), novoTotalDistribuido, estoqueFisicoTotal
                );
            }
        }

        // 2. Busca o estoque existente ou cria um novo (com quantidade 0) se for a primeira vez
        // que o produto é associado ao canal.
        Estoque estoque = estoqueRepository.findByProdutoAndCanalVenda(produto, canalVenda)
                .orElseGet(() -> criarNovoEstoque(produto, canalVenda));

        int novaQuantidade = estoque.getQuantidade() + requestDTO.getQuantidade();

        // 3. Validação de regra de negócio: o estoque de um canal não pode ficar negativo.
        if (novaQuantidade < 0) {
            throw new EstoqueInsuficienteCanalException(
                    produto.getId(),
                    canalVenda.getId(),
                    requestDTO.getQuantidade(), // A quantidade que se tentou remover
                    estoque.getQuantidade()     // O estoque atual antes da operação
            );
        }

        // 4. Atualiza e salva o estado do estoque.
        EstoqueRequestDTO updateDTO = EstoqueRequestDTO.builder()
                .produtoId(produto.getId())
                .canalVendaId(canalVenda.getId())
                .quantidade(novaQuantidade)
                .build();
        estoque.updateFrom(updateDTO, produto, canalVenda);
        Estoque estoqueSalvo = estoqueRepository.save(estoque);
        return estoqueMapper.toResponseDTO(estoqueSalvo);
    }

    /**
     * Ajusta o Estoque Físico Total de um produto ("Estoque Mestre") através de uma movimentação manual.
     * Esta operação registra uma entrada ou saída direta no estoque mestre do produto,
     * sem afetar diretamente os estoques dos canais de venda. É utilizada para correções
     * de inventário, registro de perdas, ou entradas de produção.
     *
     * @param requestDTO O DTO contendo os dados do ajuste de estoque físico (produtoId, quantidade, motivo).
     * @throws ProdutoNaoEncontradoException se o produto especificado não for encontrado.
     */
    @Override
    @Transactional
    public void ajustarEstoqueFisico(AjusteEstoqueProdutoRequestDTO requestDTO) {
        Produto produto = findProdutoById(requestDTO.getProdutoId());

        MovimentacaoEstoqueProdutoRequestDTO movimentacaoDTO = MovimentacaoEstoqueProdutoRequestDTO.builder()
                .produtoId(produto.getId())
                .tipo(TipoMovimentacaoProduto.AJUSTE_MANUAL.name())
                .quantidade(requestDTO.getQuantidade())
                .motivo(requestDTO.getMotivo())
                .build();
        MovimentacaoEstoqueProduto movimentacaoManual = MovimentacaoEstoqueProduto.from(movimentacaoDTO, produto);

        movimentacaoEstoqueProdutoRepository.save(movimentacaoManual);
    }

    /**
     * Consulta o estoque de um produto específico em um determinado canal de venda.
     *
     * @param produtoId O ID do produto a ser consultado.
     * @param canalVendaId O ID do canal de venda a ser consultado.
     * @return Um {@link EstoqueResponseDTO} representando o estoque do produto no canal.
     * @throws ProdutoNaoEncontradoException se o produto especificado não for encontrado.
     * @throws CanalVendaNaoEncontradoException se o canal de venda especificado não for encontrado.
     * @throws EstoqueNaoEncontradoException se o registro de estoque para a combinação produto/canal não existir.
     */
    @Override
    @Transactional(readOnly = true)
    public EstoqueResponseDTO consultarEstoque(Long produtoId, Long canalVendaId) {
        Produto produto = findProdutoById(produtoId);
        CanalVenda canalVenda = findCanalVendaById(canalVendaId);

        return estoqueRepository.findByProdutoAndCanalVenda(produto, canalVenda)
                .map(estoqueMapper::toResponseDTO)
                .orElseThrow(() -> new EstoqueNaoEncontradoException(produtoId, canalVendaId));
    }

    /**
     * Lista todo o histórico de movimentações ("Livro-Razão") do Estoque Físico Total de um produto.
     *
     * @param produtoId O ID do produto cujo histórico será consultado.
     * @return Uma lista de {@link MovimentacaoProdutoResponseDTO} representando todas as movimentações do produto.
     * @throws ProdutoNaoEncontradoException se o produto especificado não for encontrado.
     */
    @Override
    @Transactional(readOnly = true)
    public List<MovimentacaoProdutoResponseDTO> listarMovimentacoesPorProduto(Long produtoId) {
        Produto produto = findProdutoById(produtoId);
        return movimentacaoEstoqueProdutoRepository.findAllByProduto(produto)
                .stream()
                .map(movimentacaoProdutoMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Lista o estoque de todos os produtos, formatado para a necessidade específica do frontend.
     * O resultado é agrupado por produto, com uma lista de seus estoques em cada canal.
     */
    @Override
    @Transactional(readOnly = true)
    public List<ProdutoEstoqueResponseDTO> listarEstoqueDeTodosOsProdutosPorCanal() {
        // 1. Busca todos os registros de estoque do banco de dados.
        // 2. Agrupa os registros pelo ID do produto, criando um Map<Long, List<Estoque>>.
        // 3. Transforma cada entrada do mapa (ID do produto e sua lista de estoques) em um ProdutoEstoqueDTO.
        return estoqueRepository.findAll().stream()
                .collect(groupingBy(estoque -> estoque.getProduto().getId()))
                .entrySet().stream()
                .map(entry -> produtoEstoqueDTOMapper.toDto(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * Cria um novo registro de estoque para um produto em um canal de venda.
     * Método auxiliar para ser usado quando um registro de estoque não existe e precisa ser inicializado.
     * <p>
     * Utiliza o método from() da entidade Estoque para centralizar regras de negócio de criação.
     *
     * @param produto O produto a ser associado ao novo estoque.
     * @param canalVenda O canal de venda a ser associado ao novo estoque.
     * @return Uma nova instância de {@link Estoque} com quantidade inicial zero.
     */
    private Estoque criarNovoEstoque(Produto produto, CanalVenda canalVenda) {
        EstoqueRequestDTO dto = EstoqueRequestDTO.builder()
            .produtoId(produto.getId())
            .canalVendaId(canalVenda.getId())
            .quantidade(0)
            .build();
        return Estoque.from(dto, produto, canalVenda);
    }

    /**
     * Busca uma entidade {@link Produto} pelo seu ID.
     * Método auxiliar para evitar duplicação de código e centralizar o tratamento de "não encontrado".
     *
     * @param id O ID do produto a ser buscado.
     * @return A entidade {@link Produto} encontrada.
     * @throws ProdutoNaoEncontradoException se o produto com o ID especificado não for encontrado.
     */
    private Produto findProdutoById(Long id) {
        return produtoRepository.findById(id)
                .orElseThrow(() -> new ProdutoNaoEncontradoException(id));
    }

    /**
     * Busca uma entidade {@link CanalVenda} pelo seu ID.
     * Método auxiliar para evitar duplicação de código e centralizar o tratamento de "não encontrado".
     *
     * @param id O ID do canal de venda a ser buscado.
     * @return A entidade {@link CanalVenda} encontrada.
     * @throws CanalVendaNaoEncontradoException se o canal de venda com o ID especificado não for encontrado.
     */
    private CanalVenda findCanalVendaById(Long id) {
        return canalVendaRepository.findById(id)
                .orElseThrow(() -> new CanalVendaNaoEncontradoException(id));
    }
}
