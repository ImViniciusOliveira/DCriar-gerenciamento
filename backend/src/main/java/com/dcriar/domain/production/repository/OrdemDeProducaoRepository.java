package com.dcriar.domain.production.repository;

import com.dcriar.domain.product.entity.Produto;
import com.dcriar.domain.production.entity.OrdemDeProducao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Interface de repositório para a entidade {@link OrdemDeProducao}.
 * <p>
 * Provê métodos de acesso a dados (CRUD) para as ordens de produção,
 * abstraindo a complexidade da camada de persistência.
 */
@Repository
public interface OrdemDeProducaoRepository extends JpaRepository<OrdemDeProducao, Long> {

    /**
     * Encontra todas as ordens de produção associadas a um produto específico.
     *
     * @param produto O produto a ser verificado.
     * @return Uma lista de ordens de produção.
     */
    List<OrdemDeProducao> findAllByProduto(Produto produto);

    /**
     * Busca todas as Ordens de Produção e, em uma única query, já carrega
     * os dados das entidades relacionadas 'produto' e 'lotesConsumidos'.
     * <p>
     * Isso resolve o problema do N+1 e evita a {@link org.hibernate.LazyInitializationException}
     * ao acessar os dados fora de uma transação.
     *
     * @return Uma lista de {@link OrdemDeProducao} com as associações inicializadas.
     */
    @Query("SELECT op FROM OrdemDeProducao op JOIN FETCH op.produto JOIN FETCH op.lotesConsumidos")
    List<OrdemDeProducao> findAllWithDetails();

    /**
     * Busca uma Ordem de Produção pelo seu ID e, em uma única query, já carrega
     * os dados das entidades relacionadas 'produto' e 'lotesConsumidos'.
     *
     * @param id O ID da ordem de produção a ser buscada.
     * @return Um {@link Optional} contendo a {@link OrdemDeProducao} com as associações inicializadas, se encontrada.
     */
    @Query("SELECT op FROM OrdemDeProducao op JOIN FETCH op.produto JOIN FETCH op.lotesConsumidos WHERE op.id = :id")
    Optional<OrdemDeProducao> findByIdWithDetails(@Param("id") Long id);
}
