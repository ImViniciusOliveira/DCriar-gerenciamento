package com.dcriar.domain.product.repository;

import com.dcriar.domain.product.entity.CanalVenda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositório para a entidade CanalVenda.
 * <p>
 * Fornece as operações de CRUD (Criar, Ler, Atualizar, Deletar) básicas
 * para os canais de venda, através da abstração do Spring Data JPA.
 */
@Repository
public interface CanalVendaRepository extends JpaRepository<CanalVenda, Long> {
    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);
}
