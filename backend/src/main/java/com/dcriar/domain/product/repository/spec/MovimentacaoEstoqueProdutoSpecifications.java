package com.dcriar.domain.product.repository.spec;

import com.dcriar.domain.common.util.PostgresSearchUtils;
import com.dcriar.domain.product.entity.MovimentacaoEstoqueProduto;
import com.dcriar.domain.product.entity.enums.TipoMovimentacaoProduto;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * Fábrica de Specifications para consultas dinâmicas do histórico de movimentações de estoque.
 * Permite combinar filtros opcionais sem duplicar lógica de query no serviço.
 */
public final class MovimentacaoEstoqueProdutoSpecifications {

    private MovimentacaoEstoqueProdutoSpecifications() {
    }

    public static Specification<MovimentacaoEstoqueProduto> comPeriodo(String periodo, LocalDateTime referencia) {
        if (!StringUtils.hasText(periodo) || "all".equalsIgnoreCase(periodo)) {
            return null;
        }

        LocalDateTime inicio = switch (periodo.toLowerCase()) {
            case "1d" -> referencia.minusDays(1);
            case "1m" -> referencia.minusMonths(1);
            case "6m" -> referencia.minusMonths(6);
            case "1a" -> referencia.minusYears(1);
            default -> null;
        };

        if (inicio == null) {
            return null;
        }

        return (root, query, builder) -> builder.greaterThanOrEqualTo(root.get("data"), inicio);
    }

    public static Specification<MovimentacaoEstoqueProduto> comProdutoId(Long produtoId) {
        if (produtoId == null) {
            return null;
        }

        return (root, query, builder) -> builder.equal(root.get("produto").get("id"), produtoId);
    }

    public static Specification<MovimentacaoEstoqueProduto> comNomeProduto(String nomeProduto) {
        if (!StringUtils.hasText(nomeProduto)) {
            return null;
        }

        String termo = PostgresSearchUtils.likeTerm(nomeProduto);
        return (root, query, builder) ->
                builder.or(
                        builder.like(
                                PostgresSearchUtils.unaccentedLower(builder, root.get("produto").get("nome")),
                                termo
                        ),
                        builder.like(PostgresSearchUtils.unaccentedLower(builder, root.get("produto").get("sku")), termo)
                );
    }

    public static Specification<MovimentacaoEstoqueProduto> comTipo(TipoMovimentacaoProduto tipo) {
        if (tipo == null) {
            return null;
        }

        return (root, query, builder) -> builder.equal(root.get("tipo"), tipo);
    }
}
