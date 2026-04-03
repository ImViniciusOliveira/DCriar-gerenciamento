package com.dcriar.domain.common.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Map;

/**
 * Utilitário para padronizar ordenação estável em consultas paginadas.
 * <p>
 * Mantém o sort principal pedido pela UI e adiciona um desempate secundário
 * previsível quando necessário, evitando inversões entre itens com o mesmo valor.
 */
public final class PageableSortUtils {

    private PageableSortUtils() {
    }

    /**
     * Retorna um novo {@link Pageable} com desempate estável aplicado aos campos
     * principais configurados em {@code tieBreakers}.
     *
     * @param pageable pageable original da requisição
     * @param tieBreakers mapa de campo principal -> campo de desempate
     * @return pageable com ordenação estável
     */
    public static Pageable withStableSort(Pageable pageable, Map<String, String> tieBreakers) {
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : Sort.unsorted();
        Sort stableSort = sort;

        for (Sort.Order order : sort) {
            String tieBreaker = tieBreakers.get(order.getProperty());
            if (tieBreaker == null || stableSort.getOrderFor(tieBreaker) != null) {
                continue;
            }

            stableSort = stableSort.and(Sort.by(new Sort.Order(order.getDirection(), tieBreaker)));
        }

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), stableSort);
    }
}
