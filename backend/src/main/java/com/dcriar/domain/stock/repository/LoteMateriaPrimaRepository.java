package com.dcriar.domain.stock.repository;

import com.dcriar.domain.production.entity.OrdemDeProducao;
import com.dcriar.domain.stock.entity.LoteMateriaPrima;
import com.dcriar.domain.stock.entity.TipoMateriaPrima;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositório para a entidade {@link LoteMateriaPrima}.
 * Fornece métodos de acesso a dados e capacidade de consulta dinâmica via JpaSpecificationExecutor.
 */
@Repository
public interface LoteMateriaPrimaRepository extends JpaRepository<LoteMateriaPrima, Long>, JpaSpecificationExecutor<LoteMateriaPrima> {

    /**
     * Busca todos os lotes de matéria-prima associados a um tipo de matéria-prima específico.
     *
     * @param tipoMateriaPrima O tipo de matéria-prima para filtrar os lotes.
     * @return Uma lista de {@link LoteMateriaPrima} que correspondem ao tipo fornecido.
     */
    List<LoteMateriaPrima> findAllByTipoMateriaPrima(TipoMateriaPrima tipoMateriaPrima);

    /**
     * Busca lotes (retalhos) gerados por uma ordem de produção específica.
     *
     * @param ordemDeProducaoOrigem A ordem de produção que gerou o lote.
     * @return Lista de lotes.
     */
    List<LoteMateriaPrima> findByOrdemDeProducaoOrigem(OrdemDeProducao ordemDeProducaoOrigem);
}
