package com.dcriar.domain.product.service.impl;

import com.dcriar.api.dto.request.product.AjusteEstoqueProdutoRequestDTO;
import com.dcriar.api.dto.request.product.AjusteEstoqueRequestDTO;
import com.dcriar.api.dto.request.product.EstoqueRequestDTO;
import com.dcriar.api.dto.request.product.MovimentacaoEstoqueProdutoRequestDTO;
import com.dcriar.api.dto.response.product.AjusteEstoqueCanalResumoDTO;
import com.dcriar.api.dto.response.product.AjusteEstoqueProdutoResumoDTO;
import com.dcriar.api.dto.response.product.ConsultaEstoqueCanalResponseDTO;
import com.dcriar.api.dto.response.product.EstoqueProdutoResumoDTO;
import com.dcriar.api.dto.response.product.EstoqueResponseDTO;
import com.dcriar.api.dto.response.product.HistoricoEstoqueConsolidadoResponseDTO;
import com.dcriar.api.dto.response.product.MovimentacaoProdutoResponseDTO;
import com.dcriar.api.dto.response.product.ProdutoEstoqueResponseDTO;
import com.dcriar.api.mapper.product.EstoqueMapper;
import com.dcriar.api.mapper.product.HistoricoEstoqueConsolidadoMapper;
import com.dcriar.api.mapper.product.MovimentacaoProdutoMapper;
import com.dcriar.api.mapper.product.ProdutoEstoqueDTOMapper;
import com.dcriar.domain.common.model.CamposBloqueadosInfo;
import com.dcriar.domain.common.util.BloqueioOperacionalEstoqueUtils;
import com.dcriar.domain.common.util.PageableSortUtils;
import com.dcriar.domain.common.util.PostgresSearchUtils;
import com.dcriar.domain.common.util.StatusDivergenciaEstoqueUtils;
import com.dcriar.domain.product.entity.CanalVenda;
import com.dcriar.domain.product.entity.Estoque;
import com.dcriar.domain.product.entity.MovimentacaoEstoqueProduto;
import com.dcriar.domain.product.entity.Produto;
import com.dcriar.domain.product.entity.enums.TipoMovimentacaoProduto;
import com.dcriar.domain.product.entity.enums.DirecaoAjusteEstoque;
import com.dcriar.domain.product.repository.CanalVendaRepository;
import com.dcriar.domain.product.repository.EstoqueRepository;
import com.dcriar.domain.product.repository.MovimentacaoEstoqueProdutoRepository;
import com.dcriar.domain.product.repository.ProdutoRepository;
import com.dcriar.domain.product.repository.spec.EstoqueSpecifications;
import com.dcriar.domain.product.repository.spec.ProdutoSpecifications;
import com.dcriar.domain.product.repository.spec.MovimentacaoEstoqueProdutoSpecifications;
import com.dcriar.domain.product.service.EstoqueProdutoService;
import com.dcriar.exception.custom.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private static final Map<String, String> AJUSTE_PRODUTOS_SORT_ALIASES = Map.of(
            "nome", "nome",
            "nomeProduto", "nome",
            "sku", "sku",
            "skuProduto", "sku",
            "estoqueFisicoTotal", "estoqueFisicoTotal",
            "estoqueDistribuidoTotal", "estoqueDistribuidoTotal",
            "estoqueDisponivelParaAlocar", "estoqueDisponivelParaAlocar"
    );
    private static final Map<String, String> AJUSTE_PRODUTOS_STABLE_SORTS = Map.of(
            "nome", "id",
            "sku", "id",
            "estoqueFisicoTotal", "id",
            "estoqueDistribuidoTotal", "id",
            "estoqueDisponivelParaAlocar", "id"
    );
    private static final Map<String, String> AJUSTE_CANAIS_SORT_ALIASES = Map.ofEntries(
            Map.entry("produto.nome", "produto.nome"),
            Map.entry("nome", "produto.nome"),
            Map.entry("nomeProduto", "produto.nome"),
            Map.entry("produto.sku", "produto.sku"),
            Map.entry("sku", "produto.sku"),
            Map.entry("skuProduto", "produto.sku"),
            Map.entry("canalVenda.nome", "canalVenda.nome"),
            Map.entry("nomeCanalVenda", "canalVenda.nome"),
            Map.entry("quantidade", "quantidade"),
            Map.entry("quantidadeNoCanal", "quantidade"),
            Map.entry("estoqueFisicoTotal", "estoqueFisicoTotal"),
            Map.entry("estoqueDistribuidoTotal", "estoqueDistribuidoTotal"),
            Map.entry("estoqueDisponivelParaAlocar", "estoqueDisponivelParaAlocar")
    );
    private static final Map<String, String> AJUSTE_CANAIS_STABLE_SORTS = Map.of(
            "produto.nome", "id",
            "produto.sku", "id",
            "canalVenda.nome", "id",
            "quantidade", "id",
            "estoqueFisicoTotal", "id",
            "estoqueDistribuidoTotal", "id",
            "estoqueDisponivelParaAlocar", "id"
    );
    private static final String SORTS_ACEITOS_AJUSTE_PRODUTOS =
            "nome, nomeProduto, sku, skuProduto, estoqueFisicoTotal, estoqueDistribuidoTotal, estoqueDisponivelParaAlocar";
    private static final String SORTS_ACEITOS_AJUSTE_CANAIS =
            "produto.nome, nome, nomeProduto, produto.sku, sku, skuProduto, canalVenda.nome, nomeCanalVenda, quantidade, quantidadeNoCanal, estoqueFisicoTotal, estoqueDistribuidoTotal, estoqueDisponivelParaAlocar";

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
        int quantidadeAssinada = resolveQuantidadeAssinada(requestDTO.getDirecao(), requestDTO.getQuantidade());

        // 1. Validação de regra de negócio: ao adicionar estoque em um canal, o total distribuído
        // não pode ultrapassar o estoque físico disponível.
        if (quantidadeAssinada > 0) {
            int estoqueFisicoTotal = produto.getEstoqueFisicoTotal() != null ? produto.getEstoqueFisicoTotal() : 0;
            int totalDistribuido = produto.getEstoqueDistribuidoTotal() != null ? produto.getEstoqueDistribuidoTotal() : 0;
            int novoTotalDistribuido = totalDistribuido + quantidadeAssinada;

            if (novoTotalDistribuido > estoqueFisicoTotal) {
                throw new AlocacaoEstoqueExcedeTotalException(
                        produto.getId(),
                        formatarProdutoLabel(produto),
                        canalVenda.getId(),
                        canalVenda.getNome(),
                        quantidadeAssinada,
                        novoTotalDistribuido,
                        estoqueFisicoTotal
                );
            }
        }

        // 2. Busca o estoque existente ou cria um novo (com quantidade 0) se for a primeira vez
        // que o produto é associado ao canal.
        Estoque estoque = estoqueRepository.findByProdutoAndCanalVenda(produto, canalVenda)
                .orElseGet(() -> criarNovoEstoque(produto, canalVenda));

        int novaQuantidade = estoque.getQuantidade() + quantidadeAssinada;

        // 3. Validação de regra de negócio: o estoque de um canal não pode ficar negativo.
        if (novaQuantidade < 0) {
            throw new EstoqueInsuficienteCanalException(
                    produto.getId(),
                    formatarProdutoLabel(produto),
                    canalVenda.getId(),
                    canalVenda.getNome(),
                    quantidadeAssinada, // A quantidade que se tentou remover
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
        int quantidadeAssinada = resolveQuantidadeAssinada(requestDTO.getDirecao(), requestDTO.getQuantidade());
        CamposBloqueadosInfo bloqueiosOperacionais = BloqueioOperacionalEstoqueUtils.resolverBloqueiosOperacionaisProduto(
                produto.getEstoqueFisicoTotal(),
                produto.getEstoqueDistribuidoTotal(),
                produto.getEstoqueDisponivelParaAlocar()
        );
        if (quantidadeAssinada < 0
                && bloqueiosOperacionais.contemCampo(BloqueioOperacionalEstoqueUtils.ACAO_AJUSTE_FISICO_NEGATIVO)) {
            throw new OperacaoEstoqueBloqueadaException(
                    "PRODUTO",
                    produto.getId(),
                    formatarProdutoLabel(produto),
                    Set.of(BloqueioOperacionalEstoqueUtils.ACAO_AJUSTE_FISICO_NEGATIVO),
                    Map.of(
                            BloqueioOperacionalEstoqueUtils.ACAO_AJUSTE_FISICO_NEGATIVO,
                            bloqueiosOperacionais.motivosBloqueio().get(BloqueioOperacionalEstoqueUtils.ACAO_AJUSTE_FISICO_NEGATIVO)
                    )
            );
        }

        int estoqueFisicoAtual = produto.getEstoqueFisicoTotal() != null ? produto.getEstoqueFisicoTotal() : 0;
        int estoqueFisicoProjetado = estoqueFisicoAtual + quantidadeAssinada;
        if (quantidadeAssinada < 0 && estoqueFisicoProjetado < 0) {
            throw new EstoqueFisicoInsuficienteProdutoException(
                    produto.getId(),
                    formatarProdutoLabel(produto),
                    quantidadeAssinada,
                    estoqueFisicoAtual
            );
        }

        MovimentacaoEstoqueProdutoRequestDTO movimentacaoDTO = MovimentacaoEstoqueProdutoRequestDTO.builder()
                .produtoId(produto.getId())
                .tipo(TipoMovimentacaoProduto.AJUSTE_MANUAL.name())
                .quantidade(quantidadeAssinada)
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
                .orElseThrow(() -> new EstoqueNaoEncontradoException(
                        produtoId,
                        formatarProdutoLabel(produto),
                        canalVendaId,
                        canalVenda.getNome()
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public ConsultaEstoqueCanalResponseDTO consultarEstoqueParaConsulta(Long produtoId, Long canalVendaId) {
        Produto produto = findProdutoById(produtoId);
        CanalVenda canalVenda = findCanalVendaById(canalVendaId);

        int quantidadeNoCanal = estoqueRepository.findByProdutoAndCanalVenda(produto, canalVenda)
                .map(Estoque::getQuantidade)
                .orElse(0);

        CamposBloqueadosInfo camposBloqueados = BloqueioOperacionalEstoqueUtils.resolverBloqueiosOperacionaisCanal(
                quantidadeNoCanal,
                produto.getEstoqueFisicoTotal(),
                produto.getEstoqueDistribuidoTotal(),
                produto.getEstoqueDisponivelParaAlocar()
        );

        return ConsultaEstoqueCanalResponseDTO.builder()
                .produtoId(produto.getId())
                .nomeProduto(produto.getNome())
                .skuProduto(produto.getSku())
                .canalVendaId(canalVenda.getId())
                .nomeCanalVenda(canalVenda.getNome())
                .quantidadeNoCanal(quantidadeNoCanal)
                .estoqueFisicoTotal(produto.getEstoqueFisicoTotal())
                .estoqueDistribuidoTotal(produto.getEstoqueDistribuidoTotal())
                .estoqueDisponivelParaAlocar(produto.getEstoqueDisponivelParaAlocar())
                .statusDivergencia(StatusDivergenciaEstoqueUtils.resolverParaCanal(
                        quantidadeNoCanal,
                        produto.getEstoqueFisicoTotal(),
                        produto.getEstoqueDistribuidoTotal(),
                        produto.getEstoqueDisponivelParaAlocar()
                ))
                .camposBloqueados(camposBloqueados.camposBloqueados())
                .motivosBloqueio(camposBloqueados.motivosBloqueio())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EstoqueProdutoResumoDTO> buscarEstoqueResumido(Long canalId, String nomeProduto, boolean apenasComSaldo, Pageable pageable) {
        // Valida se o canal existe antes de buscar
        if (!canalVendaRepository.existsById(canalId)) {
            throw new CanalVendaNaoEncontradoException(canalId);
        }
        Pageable pageableComDesempate = PageableSortUtils.withStableSort(pageable, RESUMO_STABLE_SORTS);
        String nomeProdutoTermo = resolveNomeProdutoTermo(nomeProduto);
        return estoqueRepository.buscarEstoqueResumido(canalId, nomeProdutoTermo, apenasComSaldo, pageableComDesempate);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AjusteEstoqueProdutoResumoDTO> listarProdutosParaAjuste(String nomeProduto, String tipoProduto, Pageable pageable) {
        if (tipoProduto != null
                && !tipoProduto.isBlank()
                && !tipoProduto.equalsIgnoreCase("CORTE")
                && !tipoProduto.equalsIgnoreCase("CONSUMO")) {
            throw new TipoProdutoInvalidoException(tipoProduto);
        }

        Specification<Produto> spec = (root, query, builder) -> builder.conjunction();
        spec = spec.and(ProdutoSpecifications.comNomeLike(nomeProduto));
        spec = spec.and(ProdutoSpecifications.comTipo(tipoProduto));

        Pageable pageableComSortTraduzido = translatePageable(
                pageable,
                AJUSTE_PRODUTOS_SORT_ALIASES,
                "ajustes-produtos",
                SORTS_ACEITOS_AJUSTE_PRODUTOS
        );
        Pageable pageableComDesempate = PageableSortUtils.withStableSort(pageableComSortTraduzido, AJUSTE_PRODUTOS_STABLE_SORTS);
        return produtoRepository.findAll(spec, pageableComDesempate)
                .map(this::mapProdutoParaAjusteResumo);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AjusteEstoqueCanalResumoDTO> listarCanaisParaAjuste(String nomeProduto, Long canalVendaId, Pageable pageable) {
        if (canalVendaId != null && !canalVendaRepository.existsById(canalVendaId)) {
            throw new CanalVendaNaoEncontradoException(canalVendaId);
        }

        Specification<Estoque> spec = (root, query, builder) -> builder.conjunction();
        spec = spec.and(EstoqueSpecifications.comNomeProdutoLike(nomeProduto));
        spec = spec.and(EstoqueSpecifications.comCanalVendaId(canalVendaId));

        Pageable pageableComSortTraduzido = translatePageable(
                pageable,
                AJUSTE_CANAIS_SORT_ALIASES,
                "ajustes-canais",
                SORTS_ACEITOS_AJUSTE_CANAIS
        );
        Pageable pageableComDesempate = PageableSortUtils.withStableSort(pageableComSortTraduzido, AJUSTE_CANAIS_STABLE_SORTS);
        return estoqueRepository.findAll(spec, pageableComDesempate)
                .map(this::mapEstoqueParaAjusteCanalResumo);
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

    private String formatarProdutoLabel(Produto produto) {
        return produto.getSku() + " - " + produto.getNome();
    }

    private int resolveQuantidadeAssinada(DirecaoAjusteEstoque direcao, Integer quantidade) {
        int quantidadeAbsoluta = quantidade != null ? quantidade : 0;
        if (direcao == DirecaoAjusteEstoque.RETIRAR) {
            return quantidadeAbsoluta * -1;
        }
        return quantidadeAbsoluta;
    }

    private AjusteEstoqueProdutoResumoDTO mapProdutoParaAjusteResumo(Produto produto) {
        CamposBloqueadosInfo camposBloqueados = BloqueioOperacionalEstoqueUtils.resolverBloqueiosOperacionaisProduto(
                produto.getEstoqueFisicoTotal(),
                produto.getEstoqueDistribuidoTotal(),
                produto.getEstoqueDisponivelParaAlocar()
        );
        return AjusteEstoqueProdutoResumoDTO.builder()
                .produtoId(produto.getId())
                .nomeProduto(produto.getNome())
                .skuProduto(produto.getSku())
                .estoqueFisicoTotal(produto.getEstoqueFisicoTotal())
                .estoqueDistribuidoTotal(produto.getEstoqueDistribuidoTotal())
                .estoqueDisponivelParaAlocar(produto.getEstoqueDisponivelParaAlocar())
                .statusDivergencia(StatusDivergenciaEstoqueUtils.resolverParaProduto(
                        produto.getEstoqueFisicoTotal(),
                        produto.getEstoqueDistribuidoTotal(),
                        produto.getEstoqueDisponivelParaAlocar()
                ))
                .camposBloqueados(camposBloqueados.camposBloqueados())
                .motivosBloqueio(camposBloqueados.motivosBloqueio())
                .build();
    }

    private AjusteEstoqueCanalResumoDTO mapEstoqueParaAjusteCanalResumo(Estoque estoque) {
        Produto produto = estoque.getProduto();
        CamposBloqueadosInfo camposBloqueados = BloqueioOperacionalEstoqueUtils.resolverBloqueiosOperacionaisCanal(
                estoque.getQuantidade(),
                estoque.getEstoqueFisicoTotal(),
                estoque.getEstoqueDistribuidoTotal(),
                estoque.getEstoqueDisponivelParaAlocar()
        );

        return AjusteEstoqueCanalResumoDTO.builder()
                .produtoId(produto.getId())
                .nomeProduto(produto.getNome())
                .skuProduto(produto.getSku())
                .canalVendaId(estoque.getCanalVenda().getId())
                .nomeCanalVenda(estoque.getCanalVenda().getNome())
                .quantidadeNoCanal(estoque.getQuantidade())
                .estoqueFisicoTotal(estoque.getEstoqueFisicoTotal())
                .estoqueDistribuidoTotal(estoque.getEstoqueDistribuidoTotal())
                .estoqueDisponivelParaAlocar(estoque.getEstoqueDisponivelParaAlocar())
                .statusDivergencia(StatusDivergenciaEstoqueUtils.resolverParaCanal(
                        estoque.getQuantidade(),
                        estoque.getEstoqueFisicoTotal(),
                        estoque.getEstoqueDistribuidoTotal(),
                        estoque.getEstoqueDisponivelParaAlocar()
                ))
                .camposBloqueados(camposBloqueados.camposBloqueados())
                .motivosBloqueio(camposBloqueados.motivosBloqueio())
                .build();
    }

    private Pageable translatePageable(
            Pageable pageable,
            Map<String, String> aliases,
            String recurso,
            String camposAceitos
    ) {
        if (!pageable.getSort().isSorted()) {
            return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.unsorted());
        }

        List<Sort.Order> translatedOrders = pageable.getSort().stream()
                .map(order -> {
                    String translatedProperty = aliases.get(order.getProperty());
                    if (translatedProperty == null) {
                        throw new OrdenacaoInvalidaException(recurso, order.getProperty(), camposAceitos);
                    }
                    return new Sort.Order(order.getDirection(), translatedProperty);
                })
                .toList();

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(translatedOrders));
    }

    private String resolveNomeProdutoTermo(String nomeProduto) {
        if (nomeProduto == null || nomeProduto.isBlank()) {
            return null;
        }
        return PostgresSearchUtils.likeTerm(nomeProduto);
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
