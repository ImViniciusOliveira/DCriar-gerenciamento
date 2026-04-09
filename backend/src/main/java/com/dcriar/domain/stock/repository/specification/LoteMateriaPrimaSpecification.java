package com.dcriar.domain.stock.repository.specification;

import com.dcriar.api.dto.request.stock.TipoEstruturalLoteFiltro;
import com.dcriar.domain.common.util.PostgresSearchUtils;
import com.dcriar.domain.stock.entity.LoteMateriaPrima;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Fornece especificações (critérios de busca) dinâmicas para a entidade {@link LoteMateriaPrima}.
 * <p>
 * Esta classe utiliza a API de Criteria do JPA para construir queries de forma programática e segura,
 * permitindo a combinação de múltiplos filtros opcionais.
 */
public final class LoteMateriaPrimaSpecification {

    /**
     * Construtor privado para impedir a instanciação, já que esta é uma classe utilitária.
     */
    private LoteMateriaPrimaSpecification() {}

    /**
     * Cria uma {@link Specification} que combina múltiplos filtros opcionais para a busca de lotes.
     *
     * @param tipoMateriaPrimaId O ID do tipo de matéria-prima para filtrar (opcional).
     * @param apenasLotesPrincipais Se true, filtra apenas lotes que não são sobras (loteDeOrigemId é nulo) (opcional).
     * @return Uma {@link Specification} que pode ser usada diretamente pelo repositório do Spring Data JPA.
     */
    public static Specification<LoteMateriaPrima> comFiltros(Long tipoMateriaPrimaId, Boolean apenasLotesPrincipais) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // ETAPA 1: Adicionar filtro por tipo de matéria-prima, se fornecido.
            if (tipoMateriaPrimaId != null) {
                predicates.add(criteriaBuilder.equal(root.get("tipoMateriaPrima").get("id"), tipoMateriaPrimaId));
            }

            // ETAPA 2: Adicionar filtro para lotes principais, se solicitado.
            if (apenasLotesPrincipais != null && apenasLotesPrincipais) {
                predicates.add(criteriaBuilder.isNull(root.get("loteDeOrigem")));
            }

            // ETAPA 3: Combinar todos os predicados com um "AND" lógico.
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<LoteMateriaPrima> comFiltrosAjuste(
            String nomeMateriaPrima,
            TipoEstruturalLoteFiltro tipoEstrutural
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (nomeMateriaPrima != null && !nomeMateriaPrima.isBlank()) {
                String termo = PostgresSearchUtils.likeTerm(nomeMateriaPrima);
                predicates.add(
                        criteriaBuilder.like(
                                PostgresSearchUtils.unaccentedLower(criteriaBuilder, root.get("tipoMateriaPrima").get("nome")),
                                termo
                        )
                );
            }

            if (tipoEstrutural != null) {
                switch (tipoEstrutural) {
                    case LOTE_PRINCIPAL -> predicates.add(criteriaBuilder.isNull(root.get("loteDeOrigem")));
                    case RETALHO -> predicates.add(criteriaBuilder.isNotNull(root.get("loteDeOrigem")));
                }
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
