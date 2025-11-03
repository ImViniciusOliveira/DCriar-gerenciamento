package com.dcriar.domain.sales.repository;

import com.dcriar.domain.sales.entity.Venda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositório para a entidade Sale (Venda).
 * <p>
 * Fornece as operações de CRUD (Criar, Ler, Atualizar, Deletar) básicas
 * para as vendas, através da abstração do Spring Data JPA.
 */
@Repository
public interface VendaRepository extends JpaRepository<Venda, Long> {
}

