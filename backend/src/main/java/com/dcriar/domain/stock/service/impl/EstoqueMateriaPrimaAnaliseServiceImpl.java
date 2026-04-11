package com.dcriar.domain.stock.service.impl;

import com.dcriar.api.dto.request.stock.PoliticaSaldoRetalhoAnaliseFiltro;
import com.dcriar.api.dto.response.stock.AnaliseEstoqueMateriaPrimaResponseDTO;
import com.dcriar.domain.common.util.PageableSortUtils;
import com.dcriar.domain.stock.entity.LoteMateriaPrima;
import com.dcriar.domain.stock.entity.TipoMateriaPrima;
import com.dcriar.domain.stock.entity.enums.StatusAnaliseMateriaPrima;
import com.dcriar.domain.stock.entity.enums.UnidadeDeMedida;
import com.dcriar.domain.stock.repository.LoteMateriaPrimaRepository;
import com.dcriar.domain.stock.repository.TipoMateriaPrimaRepository;
import com.dcriar.domain.stock.repository.TipoMateriaPrimaSpecification;
import com.dcriar.domain.stock.service.EstoqueMateriaPrimaAnaliseService;
import com.dcriar.exception.custom.OrdenacaoInvalidaException;
import com.dcriar.exception.custom.TipoProdutoInvalidoException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class EstoqueMateriaPrimaAnaliseServiceImpl implements EstoqueMateriaPrimaAnaliseService {
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
    private static final Map<String, String> STABLE_SORTS = Map.of(
            "nome", "id",
            "unidadeDeConsumo", "id",
            "id", "id"
    );
    private static final Map<String, String> SORT_ALIASES = Map.of(
            "nome", "nome",
            "nomeTipoMateriaPrima", "nome",
            "unidadeDeConsumo", "unidadeDeConsumo",
            "id", "id",
            "tipoMateriaPrimaId", "id"
    );
    private static final String SORTS_ACEITOS =
            "nome, nomeTipoMateriaPrima, unidadeDeConsumo, id, tipoMateriaPrimaId";

    private final TipoMateriaPrimaRepository tipoMateriaPrimaRepository;
    private final LoteMateriaPrimaRepository loteMateriaPrimaRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<AnaliseEstoqueMateriaPrimaResponseDTO> listarAnalise(
            Long tipoMateriaPrimaId,
            String nome,
            UnidadeDeMedida unidadeDeConsumo,
            String tipoProduto,
            PoliticaSaldoRetalhoAnaliseFiltro politicaSaldoRetalho,
            Pageable pageable
    ) {
        validarTipoProduto(tipoProduto);
        Pageable pageableComSortTraduzido = translatePageable(pageable);

        Specification<TipoMateriaPrima> spec = Stream.of(
                        TipoMateriaPrimaSpecification.comId(tipoMateriaPrimaId),
                        TipoMateriaPrimaSpecification.comNomeSemelhante(nome),
                        TipoMateriaPrimaSpecification.comUnidadeDeConsumo(unidadeDeConsumo),
                        TipoMateriaPrimaSpecification.compativelComTipoProduto(tipoProduto)
                )
                .filter(Objects::nonNull)
                .reduce(Specification::and)
                .orElse(null);

        Pageable pageableComDesempate = PageableSortUtils.withStableSort(pageableComSortTraduzido, STABLE_SORTS);
        Page<TipoMateriaPrima> tiposPage = tipoMateriaPrimaRepository.findAll(spec, pageableComDesempate);

        List<Long> tipoIds = tiposPage.getContent().stream()
                .map(TipoMateriaPrima::getId)
                .toList();

        Map<Long, List<LoteMateriaPrima>> lotesPorTipo = tipoIds.isEmpty()
                ? Collections.emptyMap()
                : loteMateriaPrimaRepository.findAllByTipoMateriaPrimaIdsWithTipo(tipoIds).stream()
                        .collect(Collectors.groupingBy(lote -> lote.getTipoMateriaPrima().getId()));

        return tiposPage.map(tipo -> toAnaliseDto(
                tipo,
                lotesPorTipo.getOrDefault(tipo.getId(), List.of()),
                politicaSaldoRetalho != null ? politicaSaldoRetalho : PoliticaSaldoRetalhoAnaliseFiltro.TODOS
        ));
    }

    private AnaliseEstoqueMateriaPrimaResponseDTO toAnaliseDto(
            TipoMateriaPrima tipo,
            List<LoteMateriaPrima> lotes,
            PoliticaSaldoRetalhoAnaliseFiltro politicaSaldoRetalho
    ) {
        BigDecimal saldoLotesPrincipais = somarSaldoConvertido(tipo, lotes, true);
        BigDecimal saldoRetalhos = somarSaldoConvertido(tipo, lotes, false);
        BigDecimal saldoTotal = saldoLotesPrincipais.add(saldoRetalhos);
        BigDecimal saldoConsiderado = switch (politicaSaldoRetalho) {
            case SEM_RETALHOS -> saldoLotesPrincipais;
            case APENAS_RETALHOS -> saldoRetalhos;
            case TODOS -> saldoTotal;
        };

        BigDecimal estoqueCritico = scale(tipo.getEstoqueCritico());
        StatusAnaliseMateriaPrima statusAnalise = resolverStatusAnalise(saldoConsiderado, estoqueCritico);

        return AnaliseEstoqueMateriaPrimaResponseDTO.builder()
                .tipoMateriaPrimaId(tipo.getId())
                .nomeTipoMateriaPrima(tipo.getNome())
                .unidadeDeConsumo(tipo.getUnidadeDeConsumo())
                .unidadeDescricao(tipo.getUnidadeDeConsumo().getDescricao())
                .unidadeSimbolo(tipo.getUnidadeDeConsumo().getSimbolo())
                .tipoProdutoCompativel(tipo.getUnidadeDeConsumo().isPermiteCorte() ? "CORTE" : "CONSUMO")
                .saldoLotesPrincipais(saldoLotesPrincipais)
                .saldoRetalhos(saldoRetalhos)
                .saldoTotal(saldoTotal)
                .saldoConsiderado(saldoConsiderado)
                .quantidadeLotesPrincipais(lotes.stream().filter(this::isLotePrincipal).count())
                .quantidadeRetalhos(lotes.stream().filter(lote -> !isLotePrincipal(lote)).count())
                .estoqueCritico(estoqueCritico)
                .percentualRisco(calcularPercentualRisco(saldoConsiderado, estoqueCritico))
                .statusAnalise(statusAnalise)
                .build();
    }

    private BigDecimal somarSaldoConvertido(TipoMateriaPrima tipo, List<LoteMateriaPrima> lotes, boolean apenasLotesPrincipais) {
        return lotes.stream()
                .filter(lote -> isLotePrincipal(lote) == apenasLotesPrincipais)
                .map(lote -> converterSaldoParaUnidadePrincipal(tipo, lote))
                .reduce(ZERO, BigDecimal::add)
                .setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal converterSaldoParaUnidadePrincipal(TipoMateriaPrima tipo, LoteMateriaPrima lote) {
        BigDecimal saldoInterno = lote.getSaldoAtual();
        if (saldoInterno == null) {
            return ZERO;
        }
        return scale(tipo.getUnidadeDeConsumo().normalizarQuantidadeDeConsumo(saldoInterno, lote.getUnidadeDeEstoque()));
    }

    private boolean isLotePrincipal(LoteMateriaPrima lote) {
        return lote.getLoteDeOrigem() == null;
    }

    private BigDecimal calcularPercentualRisco(
            BigDecimal saldoConsiderado,
            BigDecimal estoqueCritico
    ) {
        if (estoqueCritico == null || estoqueCritico.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        if (saldoConsiderado.compareTo(estoqueCritico) >= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (saldoConsiderado.compareTo(BigDecimal.ZERO) <= 0) {
            return new BigDecimal("100.00");
        }

        BigDecimal faltaParaCritico = estoqueCritico.subtract(saldoConsiderado);
        return faltaParaCritico
                .multiply(new BigDecimal("100"))
                .divide(estoqueCritico, 2, RoundingMode.HALF_UP);
    }

    private StatusAnaliseMateriaPrima resolverStatusAnalise(
            BigDecimal saldoConsiderado,
            BigDecimal estoqueCritico
    ) {
        if (estoqueCritico == null) {
            return StatusAnaliseMateriaPrima.SEM_PARAMETRIZACAO;
        }
        if (saldoConsiderado.compareTo(estoqueCritico) <= 0) {
            return StatusAnaliseMateriaPrima.CRITICO;
        }
        return StatusAnaliseMateriaPrima.ACEITAVEL;
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
                                "analise-materias-primas",
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
                Sort.by(translatedOrders)
        );
    }

    private BigDecimal scale(BigDecimal value) {
        return value == null ? null : value.setScale(4, RoundingMode.HALF_UP);
    }
}
