package com.dcriar.domain.product.repository.spec;

import com.dcriar.domain.common.util.PostgresSearchUtils;
import com.dcriar.domain.product.entity.Estoque;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class EstoqueSpecifications {

    private EstoqueSpecifications() {}

    public static Specification<Estoque> comNomeProdutoLike(String nomeProduto) {
        if (!StringUtils.hasText(nomeProduto)) {
            return null;
        }

        String termo = PostgresSearchUtils.likeTerm(nomeProduto);
        return (root, query, builder) ->
                builder.or(
                        builder.like(PostgresSearchUtils.unaccentedLower(builder, root.get("produto").get("nome")), termo),
                        builder.like(PostgresSearchUtils.unaccentedLower(builder, root.get("produto").get("sku")), termo)
                );
    }

    public static Specification<Estoque> comCanalVendaId(Long canalVendaId) {
        if (canalVendaId == null) {
            return null;
        }

        return (root, query, builder) -> builder.equal(root.get("canalVenda").get("id"), canalVendaId);
    }
}
