package com.dcriar.domain.product.service.impl;

import com.dcriar.api.dto.response.product.AnaliseEstoqueProdutoResponseDTO;
import com.dcriar.domain.common.util.PageableSortUtils;
import com.dcriar.domain.product.entity.Produto;
import com.dcriar.domain.product.entity.enums.StatusAnaliseProduto;
import com.dcriar.domain.product.repository.ProdutoRepository;
import com.dcriar.domain.product.repository.spec.ProdutoSpecifications;
import com.dcriar.domain.product.service.EstoqueProdutoAnaliseService;
import com.dcriar.exception.custom.OrdenacaoInvalidaException;
import com.dcriar.exception.custom.TipoProdutoInvalidoException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class EstoqueProdutoAnaliseServiceImpl implements EstoqueProdutoAnaliseService {
    private static final Map<String, String> STABLE_SORTS = Map.of(
            "nome", "id",
            "sku", "id",
            "tipoProduto", "id",
            "saldoConsiderado", "id",
            "estoqueFisicoTotal", "id",
            "estoqueDistribuidoTotal", "id",
            "estoqueDisponivelParaAlocar", "id",
            "estoqueCritico", "id",
            "percentualRisco", "id",
            "statusAnalise", "id",
            "id", "id"
    );
    private static final Map<String, String> SORT_ALIASES = Map.ofEntries(
            Map.entry("nome", "nome"),
            Map.entry("nomeProduto", "nome"),
            Map.entry("sku", "sku"),
            Map.entry("skuProduto", "sku"),
            Map.entry("tipoProduto", "tipoProduto"),
            Map.entry("id", "id"),
            Map.entry("produtoId", "id"),
            Map.entry("saldoAtual", "saldoConsiderado"),
            Map.entry("saldoConsiderado", "saldoConsiderado"),
            Map.entry("estoqueFisicoTotal", "estoqueFisicoTotal"),
            Map.entry("estoqueDistribuidoTotal", "estoqueDistribuidoTotal"),
            Map.entry("estoqueDisponivelParaAlocar", "estoqueDisponivelParaAlocar"),
            Map.entry("estoqueCritico", "estoqueCritico"),
            Map.entry("percentualRisco", "percentualRisco"),
            Map.entry("statusAnalise", "statusAnalise")
    );
    private static final String SORTS_ACEITOS =
            "nome, nomeProduto, sku, skuProduto, tipoProduto, id, produtoId, saldoAtual, saldoConsiderado, estoqueFisicoTotal, estoqueDistribuidoTotal, estoqueDisponivelParaAlocar, estoqueCritico, percentualRisco, statusAnalise";

    private final ProdutoRepository produtoRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<AnaliseEstoqueProdutoResponseDTO> listarAnalise(
            Long produtoId,
            String nome,
            String tipoProduto,
            StatusAnaliseProduto statusAnalise,
            Pageable pageable
    ) {
        validarTipoProduto(tipoProduto);

        Specification<Produto> spec = Stream.of(
                        ProdutoSpecifications.comId(produtoId),
                        ProdutoSpecifications.comNomeLike(nome),
                        ProdutoSpecifications.comTipo(tipoProduto)
                )
                .filter(Objects::nonNull)
                .reduce(Specification::and)
                .orElse(null);

        List<AnaliseEstoqueProdutoResponseDTO> analises = produtoRepository.findAll(spec).stream()
                .map(this::toAnaliseDto)
                .filter(analise -> statusAnalise == null || analise.getStatusAnalise() == statusAnalise)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));

        Pageable pageableComSortTraduzido = translatePageable(pageable);
        ordenarAnalises(analises, pageableComSortTraduzido.getSort());

        int start = Math.toIntExact(pageable.getOffset());
        int end = Math.min(start + pageable.getPageSize(), analises.size());
        List<AnaliseEstoqueProdutoResponseDTO> content = start >= analises.size()
                ? List.of()
                : analises.subList(start, end);

        return new PageImpl<>(content, pageable, analises.size());
    }

    private AnaliseEstoqueProdutoResponseDTO toAnaliseDto(Produto produto) {
        Integer estoqueFisicoTotal = safeInt(produto.getEstoqueFisicoTotal());
        Integer saldoConsiderado = estoqueFisicoTotal;
        Integer estoqueDistribuidoTotal = safeInt(produto.getEstoqueDistribuidoTotal());
        Integer estoqueDisponivelParaAlocar = safeInt(produto.getEstoqueDisponivelParaAlocar());
        Integer estoqueCritico = produto.getEstoqueCritico();

        return AnaliseEstoqueProdutoResponseDTO.builder()
                .produtoId(produto.getId())
                .nomeProduto(produto.getNome())
                .skuProduto(produto.getSku())
                .tipoProduto(produto.getTipoProduto())
                .estoqueFisicoTotal(estoqueFisicoTotal)
                .saldoConsiderado(saldoConsiderado)
                .estoqueDistribuidoTotal(estoqueDistribuidoTotal)
                .estoqueDisponivelParaAlocar(estoqueDisponivelParaAlocar)
                .estoqueCritico(estoqueCritico)
                .percentualRisco(calcularPercentualRisco(saldoConsiderado, estoqueCritico))
                .statusAnalise(resolverStatusAnalise(saldoConsiderado, estoqueCritico))
                .build();
    }

    private Integer safeInt(Integer value) {
        return value != null ? value : 0;
    }

    private BigDecimal calcularPercentualRisco(Integer saldoAtual, Integer estoqueCritico) {
        if (estoqueCritico == null || estoqueCritico <= 0) {
            return null;
        }
        if (saldoAtual >= estoqueCritico) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (saldoAtual <= 0) {
            return new BigDecimal("100.00");
        }

        BigDecimal faltaParaCritico = BigDecimal.valueOf(estoqueCritico.longValue() - saldoAtual.longValue());
        return faltaParaCritico
                .multiply(new BigDecimal("100"))
                .divide(BigDecimal.valueOf(estoqueCritico.longValue()), 2, RoundingMode.HALF_UP);
    }

    private StatusAnaliseProduto resolverStatusAnalise(Integer saldoAtual, Integer estoqueCritico) {
        if (estoqueCritico == null) {
            return StatusAnaliseProduto.SEM_PARAMETRIZACAO;
        }
        if (saldoAtual <= estoqueCritico) {
            return StatusAnaliseProduto.CRITICO;
        }
        return StatusAnaliseProduto.ACEITAVEL;
    }

    private void validarTipoProduto(String tipoProduto) {
        if (tipoProduto == null || tipoProduto.isBlank()) {
            return;
        }

        if (!"CORTE".equalsIgnoreCase(tipoProduto) && !"CONSUMO".equalsIgnoreCase(tipoProduto)) {
            throw new TipoProdutoInvalidoException(tipoProduto);
        }
    }

    private Pageable translatePageable(Pageable pageable) {
        if (!pageable.getSort().isSorted()) {
            return org.springframework.data.domain.PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.unsorted());
        }

        List<Sort.Order> translatedOrders = pageable.getSort().stream()
                .map(order -> {
                    String translatedProperty = SORT_ALIASES.get(order.getProperty());
                    if (translatedProperty == null) {
                        throw new OrdenacaoInvalidaException(
                                "analise-produtos",
                                order.getProperty(),
                                SORTS_ACEITOS
                        );
                    }
                    return new Sort.Order(order.getDirection(), translatedProperty);
                })
                .toList();

        return org.springframework.data.domain.PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                PageableSortUtils.withStableSort(
                        org.springframework.data.domain.PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(translatedOrders)),
                        STABLE_SORTS
                ).getSort()
        );
    }

    private void ordenarAnalises(List<AnaliseEstoqueProdutoResponseDTO> analises, Sort sort) {
        if (!sort.isSorted()) {
            analises.sort(Comparator.comparing(AnaliseEstoqueProdutoResponseDTO::getNomeProduto, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(AnaliseEstoqueProdutoResponseDTO::getProdutoId));
            return;
        }

        Comparator<AnaliseEstoqueProdutoResponseDTO> comparator = null;
        for (Sort.Order order : sort) {
            Comparator<AnaliseEstoqueProdutoResponseDTO> next = comparatorFor(order.getProperty(), order.isAscending());
            comparator = comparator == null ? next : comparator.thenComparing(next);
        }

        if (comparator != null) {
            analises.sort(comparator);
        }
    }

    private Comparator<AnaliseEstoqueProdutoResponseDTO> comparatorFor(String property, boolean ascending) {
        Comparator<AnaliseEstoqueProdutoResponseDTO> comparator = switch (property) {
            case "nome" -> Comparator.comparing(AnaliseEstoqueProdutoResponseDTO::getNomeProduto, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "sku" -> Comparator.comparing(AnaliseEstoqueProdutoResponseDTO::getSkuProduto, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "tipoProduto" -> Comparator.comparing(AnaliseEstoqueProdutoResponseDTO::getTipoProduto, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "id" -> Comparator.comparing(AnaliseEstoqueProdutoResponseDTO::getProdutoId, Comparator.nullsLast(Long::compareTo));
            case "saldoConsiderado" -> Comparator.comparing(AnaliseEstoqueProdutoResponseDTO::getSaldoConsiderado, Comparator.nullsLast(Integer::compareTo));
            case "estoqueFisicoTotal" -> Comparator.comparing(AnaliseEstoqueProdutoResponseDTO::getEstoqueFisicoTotal, Comparator.nullsLast(Integer::compareTo));
            case "estoqueDistribuidoTotal" -> Comparator.comparing(AnaliseEstoqueProdutoResponseDTO::getEstoqueDistribuidoTotal, Comparator.nullsLast(Integer::compareTo));
            case "estoqueDisponivelParaAlocar" -> Comparator.comparing(AnaliseEstoqueProdutoResponseDTO::getEstoqueDisponivelParaAlocar, Comparator.nullsLast(Integer::compareTo));
            case "estoqueCritico" -> Comparator.comparing(AnaliseEstoqueProdutoResponseDTO::getEstoqueCritico, Comparator.nullsLast(Integer::compareTo));
            case "percentualRisco" -> Comparator.comparing(AnaliseEstoqueProdutoResponseDTO::getPercentualRisco, Comparator.nullsLast(BigDecimal::compareTo));
            case "statusAnalise" -> Comparator.comparing(dto -> dto.getStatusAnalise() != null ? dto.getStatusAnalise().name() : null, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            default -> throw new OrdenacaoInvalidaException("analise-produtos", property, SORTS_ACEITOS);
        };

        return ascending ? comparator : comparator.reversed();
    }
}
