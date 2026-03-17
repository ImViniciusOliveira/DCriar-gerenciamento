package com.dcriar.domain.product.repository.spec;

import com.dcriar.domain.product.entity.Produto;
import com.dcriar.domain.product.entity.ProdutoDeConsumo;
import com.dcriar.domain.product.entity.ProdutoDeCorte;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/**
 * Classe utilitária que fornece métodos para criar instâncias de Specification
 * para a entidade Produto. As Specifications são usadas para construir consultas
 * dinâmicas e reutilizáveis com a JPA Criteria API.
 */
public class ProdutoSpecifications {

    /**
     * Cria uma Specification que filtra produtos por nome ou SKU.
     * A busca é case-insensitive e ignora o filtro se o termo for nulo ou vazio.
     *
     * @param nome O termo de busca para nome ou SKU.
     * @return Uma Specification para o filtro de nome/SKU.
     */
    public static Specification<Produto> comNomeLike(String nome) {
        if (!StringUtils.hasText(nome)) {
            return null; // Retorna uma specification nula se não houver nome, que será ignorada.
        }
        return (root, query, builder) ->
                builder.or(
                        builder.like(builder.lower(root.get("nome")), "%" + nome.toLowerCase() + "%"),
                        builder.like(builder.lower(root.get("sku")), "%" + nome.toLowerCase() + "%")
                );
    }

    /**
     * Cria uma Specification que filtra produtos pelo seu tipo (discriminador).
     * Ignora o filtro apenas se o tipo for nulo ou vazio.
     *
     * @param tipoProduto A string que representa o tipo ("CORTE" ou "CONSUMO").
     * @return Uma Specification para o filtro de tipo.
     */
    public static Specification<Produto> comTipo(String tipoProduto) {
        if (!StringUtils.hasText(tipoProduto)) {
            return null;
        }
        return (root, query, builder) -> {
            if ("CORTE".equalsIgnoreCase(tipoProduto)) {
                return builder.equal(root.type(), ProdutoDeCorte.class);
            } else if ("CONSUMO".equalsIgnoreCase(tipoProduto)) {
                return builder.equal(root.type(), ProdutoDeConsumo.class);
            }
            return null; // A validação de tipo inválido é feita antes, na service.
        };
    }

    /**
     * Cria uma Specification que filtra produtos pelo seu estoque físico total.
     *
     * @param estoqueValor O valor de estoque para comparação.
     * @param operador A operação de comparação ("GTE" para >=, "LTE" para <=).
     * @return Uma Specification para o filtro de estoque.
     */
    public static Specification<Produto> comEstoque(Integer estoqueValor, String operador) {
        if (estoqueValor == null || !StringUtils.hasText(operador)) {
            return null;
        }
        return (root, query, builder) -> {
            if ("GTE".equalsIgnoreCase(operador)) {
                return builder.greaterThanOrEqualTo(root.get("estoqueFisicoTotal"), estoqueValor);
            } else if ("LTE".equalsIgnoreCase(operador)) {
                return builder.lessThanOrEqualTo(root.get("estoqueFisicoTotal"), estoqueValor);
            }
            return null;
        };
    }
}
