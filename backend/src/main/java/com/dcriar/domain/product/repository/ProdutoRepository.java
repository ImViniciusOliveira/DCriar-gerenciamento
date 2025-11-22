package com.dcriar.domain.product.repository;

import com.dcriar.domain.product.entity.Produto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Interface de repositório para a entidade {@link Produto}.
 * <p>
 * Provê métodos de acesso a dados (CRUD) para produtos, abstraindo a complexidade
 * da camada de persistência.
 */
@Repository
public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    /**
     * Busca todos os produtos de forma paginada.
     * <p>
     * Esta consulta customizada com {@code LEFT JOIN FETCH} garante que a associação
     * com {@code tipoMateriaPrima} seja carregada de forma otimizada (EAGER),
     * evitando o problema N+1.
     *
     * @param pageable Objeto com as informações de paginação (não pode ser nulo).
     * @return Uma página de produtos (nunca nula).
     */
    @Override
    @NonNull
    @Query(value = "SELECT p FROM Produto p " +
                   "LEFT JOIN FETCH p.tipoMateriaPrima",
           countQuery = "SELECT count(p) FROM Produto p")
    Page<Produto> findAll(@NonNull Pageable pageable);

    /**
     * Busca um produto pelo seu ID, garantindo que a associação com {@link com.dcriar.domain.stock.entity.TipoMateriaPrima}
     * seja carregada de forma otimizada (EAGER) nesta consulta específica.
     *
     * @param id O ID do produto (não pode ser nulo).
     * @return Um {@link Optional} contendo o produto, se encontrado (nunca nulo).
     */
    @Override
    @NonNull
    @EntityGraph(attributePaths = {"tipoMateriaPrima"})
    Optional<Produto> findById(@NonNull Long id);

    /**
     * Verifica se já existe um produto com o nome especificado.
     *
     * @param nome O nome do produto a ser verificado.
     * @return {@code true} se um produto com o nome existir, {@code false} caso contrário.
     */
    boolean existsByNome(String nome);

    /**
     * Verifica se já existe um produto com o SKU (Stock Keeping Unit) especificado.
     *
     * @param sku O SKU do produto a ser verificado.
     * @return {@code true} se um produto com o SKU existir, {@code false} caso contrário.
     */
    boolean existsBySku(String sku);

    /**
     * Verifica se existe outro produto com o nome especificado, excluindo o produto com o ID fornecido.
     * Útil para validações de atualização onde o próprio produto pode manter seu nome.
     *
     * @param nome O nome do produto a ser verificado.
     * @param id O ID do produto a ser excluído da verificação.
     * @return {@code true} se outro produto com o nome existir, {@code false} caso contrário.
     */
    boolean existsByNomeAndIdNot(String nome, Long id);

    /**
     * Verifica se existe outro produto com o SKU (Stock Keeping Unit) especificado, excluindo o produto com o ID fornecido.
     * Útil para validações de atualização onde o próprio produto pode manter seu SKU.
     *
     * @param sku O SKU do produto a ser verificado.
     * @param id O ID do produto a ser excluído da verificação.
     * @return {@code true} se outro produto com o SKU existir, {@code false} caso contrário.
     */
    boolean existsBySkuAndIdNot(String sku, Long id);
}
