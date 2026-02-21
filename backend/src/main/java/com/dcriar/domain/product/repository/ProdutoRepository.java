package com.dcriar.domain.product.repository;

import com.dcriar.domain.product.entity.Produto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Repositório para a entidade Produto.
 * Estende JpaSpecificationExecutor para permitir a construção de queries dinâmicas
 * baseadas em critérios (Specifications), úteis para filtros complexos.
 */
@Repository
public interface ProdutoRepository extends JpaRepository<Produto, Long>, JpaSpecificationExecutor<Produto> {

    /**
     * Verifica se já existe um produto com o mesmo nome.
     * @param nome O nome a ser verificado.
     * @return true se o nome já existe, false caso contrário.
     */
    boolean existsByNome(String nome);

    /**
     * Verifica se já existe um produto com o mesmo SKU.
     * @param sku O SKU a ser verificado.
     * @return true se o SKU já existe, false caso contrário.
     */
    boolean existsBySku(String sku);

    /**
     * Busca produtos de forma paginada, filtrando por nome ou SKU que contenham o termo de busca.
     * A busca é case-insensitive.
     *
     * @param nome O termo a ser buscado no campo 'nome'.
     * @param sku O termo a ser buscado no campo 'sku'.
     * @param pageable Objeto com as informações de paginação.
     * @return Uma página de produtos que correspondem ao critério.
     */
    Page<Produto> findByNomeContainingIgnoreCaseOrSkuContainingIgnoreCase(String nome, String sku, Pageable pageable);
}
