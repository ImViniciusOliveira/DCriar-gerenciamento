package com.dcriar.domain.product.repository;

import com.dcriar.api.dto.response.product.EstoqueProdutoResumoDTO;
import com.dcriar.domain.product.entity.CanalVenda;
import com.dcriar.domain.product.entity.Estoque;
import com.dcriar.domain.product.entity.Produto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositório para a entidade Estoque.
 */
@Repository
public interface EstoqueRepository extends JpaRepository<Estoque, Long>, JpaSpecificationExecutor<Estoque> {

    @Override
    @EntityGraph(attributePaths = {"produto", "canalVenda"})
    Page<Estoque> findAll(Specification<Estoque> spec, Pageable pageable);

    /**
     * Busca um registro de estoque pela combinação única de produto e canal de venda.
     * O Spring Data JPA cria a query automaticamente a partir do nome do método.
     *
     * @param produto O produto a ser buscado.
     * @param canalVenda O canal de venda a ser buscado.
     * @return Um Optional contendo o registro de estoque, se encontrado.
     */
    Optional<Estoque> findByProdutoAndCanalVenda(Produto produto, CanalVenda canalVenda);

    /**
     * Busca todos os registros de estoque associados a um canal de venda.
     *
     * @param canalVenda O canal de venda.
     * @return Uma lista com todos os estoques vinculados ao canal.
     */
    List<Estoque> findAllByCanalVenda(CanalVenda canalVenda);

    /**
     * Busca todos os registros de estoque para um determinado produto, em todos os canais de venda.
     *
     * @param produto O produto cujos estoques serão buscados.
     * @return Uma lista com todos os registros de estoque encontrados para o produto.
     */
    List<Estoque> findAllByProduto(Produto produto);

    /**
     * Busca todos os registros de estoque para uma lista de IDs de produtos.
     * O @EntityGraph garante que as entidades 'produto' e 'canalVenda' sejam carregadas de forma otimizada (EAGER),
     * evitando o problema de N+1 queries na camada de serviço.
     *
     * @param produtoIds A lista de IDs dos produtos.
     * @return Uma lista de Estoque com as entidades relacionadas carregadas.
     */
    @EntityGraph(attributePaths = {"produto", "canalVenda"})
    List<Estoque> findByProdutoIdIn(List<Long> produtoIds);

    /**
     * Busca resumida de estoque filtrada por canal e nome do produto.
     * Retorna um DTO projetado para autocompletes de venda.
     *
     * @param canalId ID do canal de venda.
     * @param nomeProduto Parte do nome ou SKU do produto (opcional).
     * @param apenasComSaldo Se true, retorna apenas registros com quantidade > 0.
     * @param pageable Paginação.
     * @return Página de DTOs de resumo.
     */
    @Query("SELECT new com.dcriar.api.dto.response.product.EstoqueProdutoResumoDTO(" +
           "p.id, p.nome, p.sku, e.quantidade, pr.valor) " +
           "FROM Estoque e " +
           "JOIN e.produto p " +
           "LEFT JOIN Preco pr ON pr.produto = p " +
           "WHERE e.canalVenda.id = :canalId " +
           "AND (:nomeProdutoTermo IS NULL OR " +
           "CAST(FUNCTION('unaccent', LOWER(p.nome)) AS string) LIKE :nomeProdutoTermo OR " +
           "CAST(FUNCTION('unaccent', LOWER(p.sku)) AS string) LIKE :nomeProdutoTermo) " +
           "AND (:apenasComSaldo = false OR e.quantidade > 0)")
    Page<EstoqueProdutoResumoDTO> buscarEstoqueResumido(
            @Param("canalId") Long canalId,
            @Param("nomeProdutoTermo") String nomeProdutoTermo,
            @Param("apenasComSaldo") boolean apenasComSaldo,
            Pageable pageable);

    @Query("SELECT COALESCE(SUM(e.quantidade), 0) FROM Estoque e WHERE e.produto.id = :produtoId")
    Integer sumQuantidadeByProdutoId(@Param("produtoId") Long produtoId);
}
