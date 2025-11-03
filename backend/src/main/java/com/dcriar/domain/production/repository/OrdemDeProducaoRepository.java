package com.dcriar.domain.production.repository;

import com.dcriar.domain.product.entity.Produto;
import com.dcriar.domain.production.entity.OrdemDeProducao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

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
}
