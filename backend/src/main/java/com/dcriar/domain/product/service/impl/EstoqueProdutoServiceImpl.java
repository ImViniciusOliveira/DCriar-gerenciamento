package com.dcriar.domain.product.service.impl;

import com.dcriar.api.dto.request.product.AjusteEstoqueProdutoRequestDTO;
import com.dcriar.api.dto.request.product.AjusteEstoqueRequestDTO;
import com.dcriar.api.dto.request.product.EstoqueRequestDTO;
import com.dcriar.api.dto.request.product.MovimentacaoEstoqueProdutoRequestDTO;
import com.dcriar.api.dto.response.product.EstoqueProdutoResumoDTO;
import com.dcriar.api.dto.response.product.EstoqueResponseDTO;
import com.dcriar.api.dto.response.product.HistoricoEstoqueConsolidadoResponseDTO;
import com.dcriar.api.dto.response.product.MovimentacaoProdutoResponseDTO;
import com.dcriar.api.dto.response.product.ProdutoEstoqueResponseDTO;
import com.dcriar.api.mapper.product.EstoqueMapper;
import com.dcriar.api.mapper.product.HistoricoEstoqueConsolidadoMapper;
import com.dcriar.api.mapper.product.MovimentacaoProdutoMapper;
import com.dcriar.api.mapper.product.ProdutoEstoqueDTOMapper;
import com.dcriar.domain.common.util.PageableSortUtils;
import com.dcriar.domain.product.entity.CanalVenda;
import com.dcriar.domain.product.entity.Estoque;
import com.dcriar.domain.product.entity.MovimentacaoEstoqueProduto;
import com.dcriar.domain.product.entity.Produto;
import com.dcriar.domain.product.entity.enums.TipoMovimentacaoProduto;
import com.dcriar.domain.product.repository.CanalVendaRepository;
import com.dcriar.domain.product.repository.EstoqueRepository;
import com.dcriar.domain.product.repository.MovimentacaoEstoqueProdutoRepository;
import com.dcriar.domain.product.repository.ProdutoRepository;
import com.dcriar.domain.product.repository.spec.MovimentacaoEstoqueProdutoSpecifications;
import com.dcriar.domain.product.service.EstoqueProdutoService;
import com.dcriar.exception.custom.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
    private static final Map<String, String> HISTORICO_STABLE_SORTS = Map.of("data", "id");
    private static final Map<String, String> RESUMO_STABLE_SORTS = Map.of("produto.nome", "produto.id");

    private final EstoqueRepository estoqueRepository;
    private final ProdutoRepository produtoRepository;
    private final CanalVendaRepository canalVendaRepository;
    private final EstoqueMapper estoqueMapper;
    private final MovimentacaoEstoqueProdutoRepository movimentacaoEstoqueProdutoRepository;
    private final HistoricoEstoqueConsolidadoMapper historicoEstoqueConsolidadoMapper;
    private final MovimentacaoProdutoMapper movimentacaoProdutoMapper;
    private final ProdutoEstoqueDTOMapper produtoEstoqueDTOMapper;

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

    @Override
    @Transactional(readOnly = true)
    public EstoqueResponseDTO consultarEstoque(Long produtoId, Long canalVendaId) {
        Produto produto = findProdutoById(produtoId);
        CanalVenda canalVenda = findCanalVendaById(canalVendaId);

        return estoqueRepository.findByProdutoAndCanalVenda(produto, canalVenda)
                .map(estoqueMapper::toResponseDTO)
                .orElseThrow(() -> new EstoqueNaoEncontradoException(produtoId, canalVendaId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EstoqueProdutoResumoDTO> buscarEstoqueResumido(Long canalId, String nomeProduto, boolean apenasComSaldo, Pageable pageable) {
        // Valida se o canal existe antes de buscar
        if (!canalVendaRepository.existsById(canalId)) {
            throw new CanalVendaNaoEncontradoException(canalId);
        }
        Pageable pageableComDesempate = PageableSortUtils.withStableSort(pageable, RESUMO_STABLE_SORTS);
        return estoqueRepository.buscarEstoqueResumido(canalId, nomeProduto, apenasComSaldo, pageableComDesempate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovimentacaoProdutoResponseDTO> listarMovimentacoesPorProduto(Long produtoId) {
        Produto produto = findProdutoById(produtoId);
        return movimentacaoEstoqueProdutoRepository.findAllByProduto(produto)
                .stream()
                .map(movimentacaoProdutoMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<HistoricoEstoqueConsolidadoResponseDTO> listarHistoricoConsolidado(
            String periodo,
            Long produtoId,
            String nomeProduto,
            TipoMovimentacaoProduto tipoMovimentacao,
            Pageable pageable
    ) {
        Specification<MovimentacaoEstoqueProduto> spec = (root, query, builder) -> builder.conjunction();
        spec = spec.and(MovimentacaoEstoqueProdutoSpecifications.comPeriodo(periodo, LocalDateTime.now()));
        spec = spec.and(MovimentacaoEstoqueProdutoSpecifications.comProdutoId(produtoId));
        spec = spec.and(MovimentacaoEstoqueProdutoSpecifications.comNomeProduto(nomeProduto));
        spec = spec.and(MovimentacaoEstoqueProdutoSpecifications.comTipo(tipoMovimentacao));

        Pageable pageableComDesempate = PageableSortUtils.withStableSort(pageable, HISTORICO_STABLE_SORTS);

        return movimentacaoEstoqueProdutoRepository.findAll(spec, pageableComDesempate)
                .map(historicoEstoqueConsolidadoMapper::toResponseDTO);
    }

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

    @Override
    @Transactional(readOnly = true)
    public List<ProdutoEstoqueResponseDTO> listarEstoquePorListaDeProdutos(List<Long> produtoIds) {
        if (produtoIds == null || produtoIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. Busca todos os estoques para os produtos solicitados em UMA ÚNICA QUERY.
        // O método findByProdutoIdIn precisa ser criado no EstoqueRepository.
        List<Estoque> estoques = estoqueRepository.findByProdutoIdIn(produtoIds);

        // 2. Agrupa os estoques por ID do produto.
        Map<Long, List<Estoque>> estoquesAgrupadosPorProdutoId = estoques.stream()
                .collect(groupingBy(estoque -> estoque.getProduto().getId()));

        // 3. Mapeia o resultado para a lista de DTOs de resposta.
        return estoquesAgrupadosPorProdutoId.entrySet().stream()
                .map(entry -> produtoEstoqueDTOMapper.toDto(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    private Estoque criarNovoEstoque(Produto produto, CanalVenda canalVenda) {
        EstoqueRequestDTO dto = EstoqueRequestDTO.builder()
            .produtoId(produto.getId())
            .canalVendaId(canalVenda.getId())
            .quantidade(0)
            .build();
        return Estoque.from(dto, produto, canalVenda);
    }

    private Produto findProdutoById(Long id) {
        return produtoRepository.findById(id)
                .orElseThrow(() -> new ProdutoNaoEncontradoException(id));
    }

    private CanalVenda findCanalVendaById(Long id) {
        return canalVendaRepository.findById(id)
                .orElseThrow(() -> new CanalVendaNaoEncontradoException(id));
    }
}
