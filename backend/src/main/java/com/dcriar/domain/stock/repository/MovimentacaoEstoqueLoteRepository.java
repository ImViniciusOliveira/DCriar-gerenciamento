package com.dcriar.domain.stock.repository;

import com.dcriar.domain.production.entity.OrdemDeProducao;
import com.dcriar.domain.stock.entity.LoteMateriaPrima;
import com.dcriar.domain.stock.entity.MovimentacaoEstoqueLote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Repositório para a entidade MovimentacaoEstoqueLote.
 * <p>
 * Além das operações de CRUD padrão, fornece métodos customizados para
 * calcular o saldo de estoque de um lote e buscar o seu histórico.
 */
@Repository
public interface MovimentacaoEstoqueLoteRepository extends JpaRepository<MovimentacaoEstoqueLote, Long> {

    /**
     * Calcula o saldo de estoque atual para um determinado lote somando todas as
     * suas movimentações.
     * <p>
     * A função COALESCE é usada para garantir que, se um lote não tiver nenhuma
     * movimentação, o saldo retornado seja 0, em vez de nulo.
     *
     * @param lote O lote para o qual o saldo será calculado.
     * @return O saldo de estoque atual como um BigDecimal.
     */
    @Query("SELECT COALESCE(SUM(m.quantidade), 0) FROM MovimentacaoEstoqueLote m WHERE m.lote = :lote")
    BigDecimal findSaldoByLote(@Param("lote") LoteMateriaPrima lote);

    /**
     * Busca todo o histórico de movimentações de um lote específico.
     * <p>
     * O Spring Data JPA cria a implementação deste método automaticamente
     * com base no seu nome, gerando uma query "WHERE lote = ?".
     *
     * @param lote O lote cujo histórico será buscado.
     * @return Uma lista com todas as movimentações do lote.
     */
    List<MovimentacaoEstoqueLote> findAllByLote(LoteMateriaPrima lote);

    /**
     * Busca todas as movimentações de lote associadas a uma ordem de produção específica.
     * Usado para rastreabilidade e estorno.
     *
     * @param ordemDeProducao A ordem de produção.
     * @return Lista de movimentações.
     */
    List<MovimentacaoEstoqueLote> findByOrdemDeProducao(OrdemDeProducao ordemDeProducao);
}
