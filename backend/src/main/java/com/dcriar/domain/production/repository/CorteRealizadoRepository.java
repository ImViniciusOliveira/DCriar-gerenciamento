package com.dcriar.domain.production.repository;

import com.dcriar.domain.production.entity.CorteRealizado;
import com.dcriar.domain.production.entity.OrdemDeProducao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositório para a entidade CorteRealizado.
 * <p>
 * Permite operações de acesso a dados para cortes realizados, incluindo busca por ordem de produção.
 * Não deve conter regras de negócio, apenas consultas e persistência.
 */
@Repository
public interface CorteRealizadoRepository extends JpaRepository<CorteRealizado, Long> {
    /**
     * Busca todos os cortes realizados de uma ordem de produção.
     *
     * @param ordemDeProducao Ordem de produção associada
     * @return Lista de cortes realizados
     */
    List<CorteRealizado> findByOrdemDeProducao(OrdemDeProducao ordemDeProducao);
}
