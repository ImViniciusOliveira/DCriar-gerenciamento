package com.dcriar.domain.product.repository;

import com.dcriar.domain.production.entity.OrdemDeProducao;
import com.dcriar.domain.product.entity.MovimentacaoEstoqueProduto;
import com.dcriar.domain.product.entity.Produto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositório para a entidade MovimentacaoEstoqueProduto.
 */
@Repository
public interface MovimentacaoEstoqueProdutoRepository extends JpaRepository<MovimentacaoEstoqueProduto, Long>, JpaSpecificationExecutor<MovimentacaoEstoqueProduto> {

    /**
     * Busca todo o histórico de movimentações ("Livro-Razão") de um produto específico.
     *
     * @param produto O produto cujo histórico será buscado.
     * @return Uma lista com todas as movimentações do produto.
     */
    List<MovimentacaoEstoqueProduto> findAllByProduto(Produto produto);

    @Override
    @EntityGraph(attributePaths = "produto")
    Page<MovimentacaoEstoqueProduto> findAll(Specification<MovimentacaoEstoqueProduto> spec, Pageable pageable);

    /**
     * Busca todas as movimentações de produto associadas a uma ordem de produção específica.
     * Usado para rastreabilidade e estorno.
     *
     * @param ordemDeProducao A ordem de produção.
     * @return Lista de movimentações.
     */
    List<MovimentacaoEstoqueProduto> findByOrdemDeProducao(OrdemDeProducao ordemDeProducao);

    @Query("SELECT COALESCE(SUM(m.quantidade), 0) FROM MovimentacaoEstoqueProduto m WHERE m.produto.id = :produtoId")
    Integer sumQuantidadeByProdutoId(@Param("produtoId") Long produtoId);
}
