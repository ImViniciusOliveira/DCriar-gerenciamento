package com.dcriar.domain.product.repository;

import com.dcriar.domain.product.entity.CanalVenda;
import com.dcriar.domain.product.entity.Estoque;
import com.dcriar.domain.product.entity.Produto;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositório para a entidade Estoque.
 */
@Repository
public interface EstoqueRepository extends JpaRepository<Estoque, Long> {

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
}
