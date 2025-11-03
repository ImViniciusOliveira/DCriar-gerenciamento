package com.dcriar.domain.stock.repository;

import com.dcriar.domain.stock.entity.TipoMateriaPrima;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Repositório para a entidade {@link TipoMateriaPrima}.
 * <p>
 * Fornece os métodos de acesso a dados para o catálogo de tipos de matérias-primas,
 * utilizando a abstração do Spring Data JPA e a capacidade de consultas dinâmicas
 * com JpaSpecificationExecutor.
 */
@Repository
public interface TipoMateriaPrimaRepository extends JpaRepository<TipoMateriaPrima, Long>, JpaSpecificationExecutor<TipoMateriaPrima> {
    /**
     * Verifica se já existe um tipo de matéria-prima com o mesmo nome.
     *
     * @param nome Nome do tipo de matéria-prima.
     * @return true se existir, false caso contrário.
     */
    boolean existsByNome(String nome);
}
