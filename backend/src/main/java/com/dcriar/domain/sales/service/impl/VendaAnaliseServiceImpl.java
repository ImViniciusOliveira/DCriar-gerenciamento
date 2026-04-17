package com.dcriar.domain.sales.service.impl;

import com.dcriar.api.dto.response.sales.VendaAnalisePorCanalItemResponseDTO;
import com.dcriar.api.dto.response.sales.VendaAnalisePorProdutoItemResponseDTO;
import com.dcriar.api.dto.response.sales.VendaAnaliseSerieItemResponseDTO;
import com.dcriar.api.dto.response.sales.VendaAnaliseSerieTemporalResponseDTO;
import com.dcriar.api.dto.response.sales.VendaAnaliseTotaisResponseDTO;
import com.dcriar.domain.sales.repository.VendaRepository;
import com.dcriar.domain.sales.service.VendaAnaliseService;
import com.dcriar.exception.custom.OrdenacaoInvalidaException;
import com.dcriar.exception.custom.PeriodoAnaliseInvalidoException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VendaAnaliseServiceImpl implements VendaAnaliseService {

    private static final int LIMITE_DIAS_PERIODO = 366;

    private static final Map<String, String> SORT_ALIASES_CANAL = Map.of(
            "receita",       "receita",
            "totalPedidos",  "totalPedidos",
            "nomeCanal",     "nomeCanal",
            "canalVendaId",  "canalVendaId"
    );
    private static final String SORTS_ACEITOS_CANAL = "receita, totalPedidos, nomeCanal, canalVendaId";

    private static final Map<String, String> SORT_ALIASES_PRODUTO = Map.of(
            "receita",          "receita",
            "unidadesVendidas", "unidadesVendidas",
            "nomeProduto",      "nomeProduto",
            "skuProduto",       "skuProduto",
            "produtoId",        "produtoId"
    );
    private static final String SORTS_ACEITOS_PRODUTO = "receita, unidadesVendidas, nomeProduto, skuProduto, produtoId";

    private static final DateTimeFormatter FORMATO_DIA = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter FORMATO_MES_ANO = DateTimeFormatter.ofPattern("MMM yyyy", Locale.of("pt", "BR"));

    private final VendaRepository vendaRepository;

    @Override
    @Transactional(readOnly = true)
    public VendaAnaliseTotaisResponseDTO consultarTotais(LocalDate dataInicio, LocalDate dataFim) {
        validarPeriodo(dataInicio, dataFim);

        LocalDateTime inicio = dataInicio.atStartOfDay();
        LocalDateTime fim = dataFim.plusDays(1).atStartOfDay();

        Object[] resultado = extrairLinhaTotais(vendaRepository.consultarTotaisPorPeriodo(inicio, fim));
        BigDecimal receita = toBigDecimal(resultado[0]);
        long totalPedidos = resultado[1] != null ? ((Number) resultado[1]).longValue() : 0L;

        long diasPeriodo = ChronoUnit.DAYS.between(dataInicio, dataFim) + 1;
        LocalDateTime inicioAnterior = dataInicio.minusDays(diasPeriodo).atStartOfDay();
        LocalDateTime fimAnterior = dataInicio.atStartOfDay();

        Object[] resultadoAnterior = extrairLinhaTotais(vendaRepository.consultarTotaisPorPeriodo(inicioAnterior, fimAnterior));
        BigDecimal receitaAnterior = toBigDecimal(resultadoAnterior[0]);

        return VendaAnaliseTotaisResponseDTO.builder()
                .receita(receita)
                .totalPedidos(totalPedidos)
                .receitaPeriodoAnterior(receitaAnterior)
                .deltaPercent(calcularDeltaPercent(receita, receitaAnterior))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VendaAnalisePorCanalItemResponseDTO> consultarPorCanal(LocalDate dataInicio, LocalDate dataFim, Pageable pageable) {
        validarPeriodo(dataInicio, dataFim);

        LocalDateTime inicio = dataInicio.atStartOfDay();
        LocalDateTime fim = dataFim.plusDays(1).atStartOfDay();

        List<Object[]> rows = vendaRepository.consultarPorCanalPorPeriodo(inicio, fim);
        List<VendaAnalisePorCanalItemResponseDTO> itens = new ArrayList<>();
        for (Object[] row : rows) {
            itens.add(VendaAnalisePorCanalItemResponseDTO.builder()
                    .canalVendaId(((Number) row[0]).longValue())
                    .nomeCanal((String) row[1])
                    .receita(row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO)
                    .totalPedidos(row[3] != null ? ((Number) row[3]).longValue() : 0L)
                    .build());
        }

        ordenarCanal(itens, traduzirSort(pageable.getSort(), SORT_ALIASES_CANAL, SORTS_ACEITOS_CANAL, "por-canal"));
        return paginar(itens, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VendaAnalisePorProdutoItemResponseDTO> consultarPorProduto(LocalDate dataInicio, LocalDate dataFim, Pageable pageable) {
        validarPeriodo(dataInicio, dataFim);

        LocalDateTime inicio = dataInicio.atStartOfDay();
        LocalDateTime fim = dataFim.plusDays(1).atStartOfDay();

        List<Object[]> rows = vendaRepository.consultarPorProdutoPorPeriodo(inicio, fim);
        List<VendaAnalisePorProdutoItemResponseDTO> itens = new ArrayList<>();
        for (Object[] row : rows) {
            itens.add(VendaAnalisePorProdutoItemResponseDTO.builder()
                    .produtoId(((Number) row[0]).longValue())
                    .nomeProduto((String) row[1])
                    .skuProduto((String) row[2])
                    .receita(row[3] != null ? (BigDecimal) row[3] : BigDecimal.ZERO)
                    .unidadesVendidas(row[4] != null ? ((Number) row[4]).longValue() : 0L)
                    .build());
        }

        ordenarProduto(itens, traduzirSort(pageable.getSort(), SORT_ALIASES_PRODUTO, SORTS_ACEITOS_PRODUTO, "por-produto"));
        return paginar(itens, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public VendaAnaliseSerieTemporalResponseDTO consultarSerieTemporal(LocalDate dataInicio, LocalDate dataFim) {
        validarPeriodo(dataInicio, dataFim);

        LocalDateTime inicio = dataInicio.atStartOfDay();
        LocalDateTime fim = dataFim.plusDays(1).atStartOfDay();

        List<Object[]> raw = vendaRepository.findDataCriacaoEValorTotalPorPeriodo(inicio, fim);
        long diasTotais = ChronoUnit.DAYS.between(dataInicio, dataFim) + 1;
        AgrupamentoPeriodo agrupamento = resolverAgrupamento(diasTotais);

        List<VendaAnaliseSerieItemResponseDTO> serie = agruparSerie(raw, dataInicio, dataFim, agrupamento);

        return VendaAnaliseSerieTemporalResponseDTO.builder()
                .trendLabel(agrupamento.trendLabel)
                .serie(serie)
                .build();
    }

    // region Validação

    private void validarPeriodo(LocalDate dataInicio, LocalDate dataFim) {
        if (dataInicio.isAfter(dataFim)) {
            throw PeriodoAnaliseInvalidoException.periodoInvertido(dataInicio, dataFim);
        }
        long dias = ChronoUnit.DAYS.between(dataInicio, dataFim) + 1;
        if (dias > LIMITE_DIAS_PERIODO) {
            throw PeriodoAnaliseInvalidoException.periodoExcedeLimite(dataInicio, dataFim, dias);
        }
    }

    // endregion

    // region Cálculos financeiros

    private BigDecimal calcularDeltaPercent(BigDecimal atual, BigDecimal anterior) {
        if (anterior == null || anterior.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return atual.subtract(anterior)
                .multiply(new BigDecimal("100"))
                .divide(anterior, 2, RoundingMode.HALF_UP);
    }

    // endregion

    // region Série temporal

    private enum AgrupamentoPeriodo {
        TURNO("Receita por período do dia"),
        DIA("Receita diária"),
        SEMANA("Receita semanal"),
        MES("Receita mensal");

        final String trendLabel;

        AgrupamentoPeriodo(String trendLabel) {
            this.trendLabel = trendLabel;
        }
    }

    private AgrupamentoPeriodo resolverAgrupamento(long dias) {
        if (dias == 1) {
            return AgrupamentoPeriodo.TURNO;
        }
        if (dias <= 7) {
            return AgrupamentoPeriodo.DIA;
        }
        if (dias <= 31) {
            return AgrupamentoPeriodo.SEMANA;
        }
        return AgrupamentoPeriodo.MES;
    }

    private List<VendaAnaliseSerieItemResponseDTO> agruparSerie(
            List<Object[]> raw,
            LocalDate dataInicio,
            LocalDate dataFim,
            AgrupamentoPeriodo agrupamento
    ) {
        return switch (agrupamento) {
            case TURNO -> agruparPorTurno(raw);
            case DIA -> agruparPorDia(raw, dataInicio, dataFim);
            case SEMANA -> agruparPorSemana(raw, dataInicio, dataFim);
            case MES -> agruparPorMes(raw, dataInicio, dataFim);
        };
    }

    private List<VendaAnaliseSerieItemResponseDTO> agruparPorTurno(List<Object[]> raw) {
        Map<String, BigDecimal[]> buckets = new LinkedHashMap<>();
        Map<String, String> helperLabels = new LinkedHashMap<>();

        buckets.put("Manhã", new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        buckets.put("Tarde", new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        buckets.put("Noite", new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});

        helperLabels.put("Manhã", "00:00 a 11:59");
        helperLabels.put("Tarde", "12:00 a 17:59");
        helperLabels.put("Noite", "18:00 a 23:59");

        for (Object[] row : raw) {
            LocalDateTime dataCriacao = (LocalDateTime) row[0];
            String bucketKey = resolverTurno(dataCriacao.getHour());
            BigDecimal[] bucket = buckets.get(bucketKey);

            bucket[0] = bucket[0].add(row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO);
            bucket[1] = bucket[1].add(BigDecimal.ONE);
        }

        List<VendaAnaliseSerieItemResponseDTO> serie = new ArrayList<>();
        buckets.forEach((label, bucket) -> serie.add(VendaAnaliseSerieItemResponseDTO.builder()
                .label(label)
                .helperLabel(helperLabels.get(label))
                .receita(bucket[0])
                .totalPedidos(bucket[1].longValue())
                .build()));
        return serie;
    }

    private String resolverTurno(int hora) {
        if (hora < 12) {
            return "Manhã";
        }
        if (hora < 18) {
            return "Tarde";
        }
        return "Noite";
    }

    private List<VendaAnaliseSerieItemResponseDTO> agruparPorDia(List<Object[]> raw, LocalDate dataInicio, LocalDate dataFim) {
        Map<LocalDate, BigDecimal[]> buckets = new LinkedHashMap<>();
        for (LocalDate d = dataInicio; !d.isAfter(dataFim); d = d.plusDays(1)) {
            buckets.put(d, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        }

        for (Object[] row : raw) {
            LocalDate dia = ((LocalDateTime) row[0]).toLocalDate();
            BigDecimal[] bucket = buckets.get(dia);
            if (bucket != null) {
                bucket[0] = bucket[0].add(row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO);
                bucket[1] = bucket[1].add(BigDecimal.ONE);
            }
        }

        List<VendaAnaliseSerieItemResponseDTO> serie = new ArrayList<>();
        buckets.forEach((dia, bucket) -> serie.add(VendaAnaliseSerieItemResponseDTO.builder()
                .label(dia.format(FORMATO_DIA))
                .helperLabel(null)
                .receita(bucket[0])
                .totalPedidos(bucket[1].longValue())
                .build()));
        return serie;
    }

    private List<VendaAnaliseSerieItemResponseDTO> agruparPorSemana(List<Object[]> raw, LocalDate dataInicio, LocalDate dataFim) {
        WeekFields weekFields = WeekFields.of(Locale.of("pt", "BR"));
        Map<String, BigDecimal[]> buckets = new LinkedHashMap<>();
        Map<String, String> helperLabels = new LinkedHashMap<>();

        for (LocalDate d = dataInicio; !d.isAfter(dataFim); ) {
            LocalDate inicioSemana = d;
            LocalDate fimSemana = d.plusDays(6).isAfter(dataFim) ? dataFim : d.plusDays(6);
            int semana = d.get(weekFields.weekOfWeekBasedYear());
            String chave = d.get(weekFields.weekBasedYear()) + "-S" + semana;
            buckets.putIfAbsent(chave, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            helperLabels.putIfAbsent(chave, inicioSemana.format(FORMATO_DIA) + " a " + fimSemana.format(FORMATO_DIA));
            d = fimSemana.plusDays(1);
        }

        for (Object[] row : raw) {
            LocalDate dia = ((LocalDateTime) row[0]).toLocalDate();
            int semana = dia.get(weekFields.weekOfWeekBasedYear());
            String chave = dia.get(weekFields.weekBasedYear()) + "-S" + semana;
            BigDecimal[] bucket = buckets.get(chave);
            if (bucket != null) {
                bucket[0] = bucket[0].add(row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO);
                bucket[1] = bucket[1].add(BigDecimal.ONE);
            }
        }

        List<VendaAnaliseSerieItemResponseDTO> serie = new ArrayList<>();
        int[] semanaNum = {1};
        buckets.forEach((chave, bucket) -> {
            serie.add(VendaAnaliseSerieItemResponseDTO.builder()
                    .label("Sem " + semanaNum[0])
                    .helperLabel(helperLabels.get(chave))
                    .receita(bucket[0])
                    .totalPedidos(bucket[1].longValue())
                    .build());
            semanaNum[0]++;
        });
        return serie;
    }

    private List<VendaAnaliseSerieItemResponseDTO> agruparPorMes(List<Object[]> raw, LocalDate dataInicio, LocalDate dataFim) {
        Map<String, BigDecimal[]> buckets = new LinkedHashMap<>();
        Map<String, LocalDate> inicioDeMes = new LinkedHashMap<>();

        for (LocalDate d = dataInicio.withDayOfMonth(1); !d.isAfter(dataFim); d = d.plusMonths(1)) {
            String chave = d.getYear() + "-" + d.getMonthValue();
            buckets.putIfAbsent(chave, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            inicioDeMes.putIfAbsent(chave, d);
        }

        for (Object[] row : raw) {
            LocalDate dia = ((LocalDateTime) row[0]).toLocalDate();
            String chave = dia.getYear() + "-" + dia.getMonthValue();
            BigDecimal[] bucket = buckets.get(chave);
            if (bucket != null) {
                bucket[0] = bucket[0].add(row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO);
                bucket[1] = bucket[1].add(BigDecimal.ONE);
            }
        }

        List<VendaAnaliseSerieItemResponseDTO> serie = new ArrayList<>();
        buckets.forEach((chave, bucket) -> {
            LocalDate primeiroDia = inicioDeMes.get(chave);
            serie.add(VendaAnaliseSerieItemResponseDTO.builder()
                    .label(primeiroDia.format(FORMATO_MES_ANO))
                    .helperLabel(null)
                    .receita(bucket[0])
                    .totalPedidos(bucket[1].longValue())
                    .build());
        });
        return serie;
    }

    // endregion

    // region Ordenação e paginação

    private Sort traduzirSort(Sort sort, Map<String, String> aliases, String sortesAceitos, String recurso) {
        if (!sort.isSorted()) {
            return Sort.unsorted();
        }
        List<Sort.Order> traduzidos = new ArrayList<>();
        for (Sort.Order order : sort) {
            String traduzido = aliases.get(order.getProperty());
            if (traduzido == null) {
                throw new OrdenacaoInvalidaException(recurso, order.getProperty(), sortesAceitos);
            }
            traduzidos.add(new Sort.Order(order.getDirection(), traduzido));
        }
        return Sort.by(traduzidos);
    }

    private void ordenarCanal(List<VendaAnalisePorCanalItemResponseDTO> itens, Sort sort) {
        if (!sort.isSorted()) {
            itens.sort(Comparator.comparing(VendaAnalisePorCanalItemResponseDTO::getReceita, Comparator.nullsLast(BigDecimal::compareTo)).reversed()
                    .thenComparing(VendaAnalisePorCanalItemResponseDTO::getCanalVendaId, Comparator.nullsLast(Long::compareTo)));
            return;
        }
        Comparator<VendaAnalisePorCanalItemResponseDTO> comp = null;
        for (Sort.Order order : sort) {
            Comparator<VendaAnalisePorCanalItemResponseDTO> next = switch (order.getProperty()) {
                case "receita" -> Comparator.comparing(VendaAnalisePorCanalItemResponseDTO::getReceita, Comparator.nullsLast(BigDecimal::compareTo));
                case "totalPedidos" -> Comparator.comparing(VendaAnalisePorCanalItemResponseDTO::getTotalPedidos, Comparator.nullsLast(Long::compareTo));
                case "nomeCanal" -> Comparator.comparing(VendaAnalisePorCanalItemResponseDTO::getNomeCanal, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
                case "canalVendaId" -> Comparator.comparing(VendaAnalisePorCanalItemResponseDTO::getCanalVendaId, Comparator.nullsLast(Long::compareTo));
                default -> throw new OrdenacaoInvalidaException("por-canal", order.getProperty(), SORTS_ACEITOS_CANAL);
            };
            next = order.isAscending() ? next : next.reversed();
            comp = comp == null ? next : comp.thenComparing(next);
        }
        if (comp != null) {
            itens.sort(comp);
        }
    }

    private void ordenarProduto(List<VendaAnalisePorProdutoItemResponseDTO> itens, Sort sort) {
        if (!sort.isSorted()) {
            itens.sort(Comparator.comparing(VendaAnalisePorProdutoItemResponseDTO::getReceita, Comparator.nullsLast(BigDecimal::compareTo)).reversed()
                    .thenComparing(VendaAnalisePorProdutoItemResponseDTO::getProdutoId, Comparator.nullsLast(Long::compareTo)));
            return;
        }
        Comparator<VendaAnalisePorProdutoItemResponseDTO> comp = null;
        for (Sort.Order order : sort) {
            Comparator<VendaAnalisePorProdutoItemResponseDTO> next = switch (order.getProperty()) {
                case "receita" -> Comparator.comparing(VendaAnalisePorProdutoItemResponseDTO::getReceita, Comparator.nullsLast(BigDecimal::compareTo));
                case "unidadesVendidas" -> Comparator.comparing(VendaAnalisePorProdutoItemResponseDTO::getUnidadesVendidas, Comparator.nullsLast(Long::compareTo));
                case "nomeProduto" -> Comparator.comparing(VendaAnalisePorProdutoItemResponseDTO::getNomeProduto, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
                case "skuProduto" -> Comparator.comparing(VendaAnalisePorProdutoItemResponseDTO::getSkuProduto, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
                case "produtoId" -> Comparator.comparing(VendaAnalisePorProdutoItemResponseDTO::getProdutoId, Comparator.nullsLast(Long::compareTo));
                default -> throw new OrdenacaoInvalidaException("por-produto", order.getProperty(), SORTS_ACEITOS_PRODUTO);
            };
            next = order.isAscending() ? next : next.reversed();
            comp = comp == null ? next : comp.thenComparing(next);
        }
        if (comp != null) {
            itens.sort(comp);
        }
    }

    private <T> Page<T> paginar(List<T> itens, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), itens.size());
        List<T> conteudo = start >= itens.size() ? List.of() : itens.subList(start, end);
        return new PageImpl<>(conteudo, pageable, itens.size());
    }

    private static Object[] extrairLinhaTotais(List<Object[]> rows) {
        if (rows == null || rows.isEmpty()) {
            return new Object[]{null, null};
        }
        return rows.get(0);
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        return new BigDecimal(value.toString());
    }

    // endregion
}
