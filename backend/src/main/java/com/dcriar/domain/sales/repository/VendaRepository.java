package com.dcriar.domain.sales.repository;

import com.dcriar.domain.product.entity.CanalVenda;
import com.dcriar.domain.sales.entity.Venda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
