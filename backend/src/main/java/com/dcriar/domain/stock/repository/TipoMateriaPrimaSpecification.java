package com.dcriar.domain.stock.repository;

import com.dcriar.domain.stock.entity.TipoMateriaPrima;
import com.dcriar.domain.stock.entity.enums.UnidadeDeMedida;
import org.springframework.data.jpa.domain.Specification;

public class TipoMateriaPrimaSpecification {

    /**
     * Retorna uma Specification para filtrar por nome, usando uma busca case-insensitive (LIKE %nome%).
     *
     * @param nome O nome a ser buscado.
     * @return Uma {@link Specification} ou null se o nome for nulo ou vazio.
     */
    public static Specification<TipoMateriaPrima> comNomeSemelhante(String nome) {
        if (nome == null || nome.isBlank()) {
            return null;
        }
        return (root, query, builder) ->
                builder.like(builder.lower(root.get("nome")), "%" + nome.toLowerCase() + "%");
    }

    /**
     * Retorna uma Specification para filtrar por unidade de consumo.
     *
     * @param unidade A unidade de consumo a ser buscada.
     * @return Uma {@link Specification} ou null se a unidade for nula.
     */
    public static Specification<TipoMateriaPrima> comUnidadeDeConsumo(UnidadeDeMedida unidade) {
        if (unidade == null) {
            return null;
        }
        return (root, query, builder) ->
                builder.equal(root.get("unidadeDeConsumo"), unidade);
    }
}
