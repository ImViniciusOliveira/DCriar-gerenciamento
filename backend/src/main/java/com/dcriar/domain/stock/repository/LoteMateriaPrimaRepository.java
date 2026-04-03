package com.dcriar.domain.stock.repository;

import com.dcriar.domain.production.entity.OrdemDeProducao;
import com.dcriar.domain.stock.entity.LoteMateriaPrima;
import com.dcriar.domain.stock.entity.TipoMateriaPrima;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositório para a entidade {@link LoteMateriaPrima}.
 * Fornece métodos de acesso a dados e capacidade de consulta dinâmica via JpaSpecificationExecutor.
 */
@Repository
public interface LoteMateriaPrimaRepository extends JpaRepository<LoteMateriaPrima, Long>, JpaSpecificationExecutor<LoteMateriaPrima> {

    /**
     * Busca um lote de matéria-prima por seu ID, garantindo que a entidade
     * {@link TipoMateriaPrima} associada seja carregada de forma EAGER (imediata).
     *
     * @param id O ID do lote a ser buscado.
     * @return um {@link Optional} contendo o {@link LoteMateriaPrima} se encontrado.
     */
    @Query("SELECT l FROM LoteMateriaPrima l JOIN FETCH l.tipoMateriaPrima WHERE l.id = :id")
    Optional<LoteMateriaPrima> findByIdWithTipoMateriaPrima(@Param("id") Long id);

    /**
     * Busca todos os lotes de matéria-prima associados a um tipo de matéria-prima específico.
     *
     * @param tipoMateriaPrima O tipo de matéria-prima para filtrar os lotes.
     * @return Uma lista de {@link LoteMateriaPrima} que correspondem ao tipo fornecido.
     */
    List<LoteMateriaPrima> findAllByTipoMateriaPrima(TipoMateriaPrima tipoMateriaPrima);

    boolean existsByTipoMateriaPrima(TipoMateriaPrima tipoMateriaPrima);

    /**
     * Busca lotes (retalhos) gerados por uma ordem de produção específica.
     *
     * @param ordemDeProducaoOrigem A ordem de produção que gerou o lote.
     * @return Lista de lotes.
     */
    List<LoteMateriaPrima> findByOrdemDeProducaoOrigem(OrdemDeProducao ordemDeProducaoOrigem);

    /**
     * Busca lotes derivados diretamente de um lote principal.
     *
     * @param loteDeOrigem O lote de origem.
     * @return Lista de lotes derivados.
     */
    List<LoteMateriaPrima> findByLoteDeOrigem(LoteMateriaPrima loteDeOrigem);
}
