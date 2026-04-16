package com.dcriar.domain.sales.repository;

import com.dcriar.domain.product.entity.CanalVenda;
import com.dcriar.domain.sales.entity.Venda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositório para a entidade Sale (Venda).
 * <p>
 * Fornece as operações de CRUD (Criar, Ler, Atualizar, Deletar) básicas
 * para as vendas, através da abstração do Spring Data JPA.
 */
@Repository
public interface VendaRepository extends JpaRepository<Venda, Long> {

    /**
     * Busca todas as vendas associadas a um canal de venda.
     *
     * @param canalVenda O canal de venda.
     * @return Lista de vendas vinculadas ao canal.
     */
    List<Venda> findAllByCanalVenda(CanalVenda canalVenda);

    /**
     * Retorna a soma da receita e o total de pedidos no intervalo [inicio, fim).
     * O resultado é um Object[] com [0]=SUM(valorTotal) e [1]=COUNT(id).
     * Ambos podem ser nulos quando não há vendas no período.
     */
    @Query("SELECT SUM(v.valorTotal), COUNT(v.id) FROM Venda v WHERE v.dataCriacao >= :inicio AND v.dataCriacao < :fim")
    List<Object[]> consultarTotaisPorPeriodo(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    /**
     * Retorna receita e total de pedidos agrupados por canal de venda no intervalo [inicio, fim).
     * Cada Object[] contém: [0]=canalVendaId, [1]=nomeCanal, [2]=SUM(valorTotal), [3]=COUNT(id).
     */
    @Query("""
            SELECT v.canalVenda.id, v.canalVenda.nome, SUM(v.valorTotal), COUNT(v.id)
            FROM Venda v
            WHERE v.dataCriacao >= :inicio AND v.dataCriacao < :fim
            GROUP BY v.canalVenda.id, v.canalVenda.nome
            """)
    List<Object[]> consultarPorCanalPorPeriodo(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    /**
     * Retorna receita e unidades vendidas agrupados por produto no intervalo [inicio, fim).
     * Cada Object[] contém: [0]=produtoId, [1]=nomeProduto, [2]=skuProduto, [3]=SUM(precoTotal), [4]=SUM(quantidade).
     */
    @Query("""
            SELECT iv.produto.id, iv.produto.nome, iv.produto.sku, SUM(iv.precoTotal), SUM(iv.quantidade)
            FROM Venda v JOIN v.itens iv
            WHERE v.dataCriacao >= :inicio AND v.dataCriacao < :fim
            GROUP BY iv.produto.id, iv.produto.nome, iv.produto.sku
            """)
    List<Object[]> consultarPorProdutoPorPeriodo(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    /**
     * Retorna pares de (dataCriacao, valorTotal) de todas as vendas no intervalo [inicio, fim),
     * ordenados cronologicamente. Usado para agrupamento temporal em memória.
     */
    @Query("SELECT v.dataCriacao, v.valorTotal FROM Venda v WHERE v.dataCriacao >= :inicio AND v.dataCriacao < :fim ORDER BY v.dataCriacao ASC")
    List<Object[]> findDataCriacaoEValorTotalPorPeriodo(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);
}
