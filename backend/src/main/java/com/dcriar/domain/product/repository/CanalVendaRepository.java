package com.dcriar.domain.product.repository;

import com.dcriar.domain.product.entity.CanalVenda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repositório para a entidade CanalVenda.
 * <p>
 * Fornece as operações de CRUD (Criar, Ler, Atualizar, Deletar) básicas
 * para os canais de venda, através da abstração do Spring Data JPA.
 */
@Repository
public interface CanalVendaRepository extends JpaRepository<CanalVenda, Long> {
    @Query(
            value = """
                    SELECT EXISTS (
                        SELECT 1
                        FROM canais_venda c
                        WHERE dcriar_normalize_catalog_key(c.nome) = dcriar_normalize_catalog_key(:nome)
                    )
                    """,
            nativeQuery = true
    )
    boolean existsByNomeNormalized(@Param("nome") String nome);

    @Query(
            value = """
                    SELECT EXISTS (
                        SELECT 1
                        FROM canais_venda c
                        WHERE c.id <> :id
                          AND dcriar_normalize_catalog_key(c.nome) = dcriar_normalize_catalog_key(:nome)
                    )
                    """,
            nativeQuery = true
    )
    boolean existsByNomeNormalizedAndIdNot(@Param("nome") String nome, @Param("id") Long id);
}
