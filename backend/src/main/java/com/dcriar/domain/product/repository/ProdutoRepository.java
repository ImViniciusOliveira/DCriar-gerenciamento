package com.dcriar.domain.product.repository;

import com.dcriar.domain.product.entity.Produto;
import com.dcriar.domain.stock.entity.TipoMateriaPrima;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositório para a entidade Produto.
 * Estende JpaSpecificationExecutor para permitir a construção de queries dinâmicas
 * baseadas em critérios (Specifications), úteis para filtros complexos.
 */
@Repository
public interface ProdutoRepository extends JpaRepository<Produto, Long>, JpaSpecificationExecutor<Produto> {

    /**
     * Busca um produto pelo seu ID, forçando o carregamento da entidade {@code TipoMateriaPrima} associada.
     * <p>
     * O uso de {@code JOIN FETCH} instrui o Hibernate a carregar a entidade relacionada na mesma consulta,
     * evitando a {@code LazyInitializationException} em contextos onde a sessão do banco de dados
     * é fechada antes que os dados da matéria-prima sejam acessados (ex: em serviços não-transacionais
     * ou após o objeto ser passado para outras camadas).
     *
     * @param id O ID do produto a ser buscado.
     * @return um {@code Optional} contendo o produto com sua matéria-prima inicializada, ou vazio se não encontrado.
     */
    @Query("SELECT p FROM Produto p JOIN FETCH p.tipoMateriaPrima WHERE p.id = :id")
    Optional<Produto> findByIdWithTipoMateriaPrima(@Param("id") Long id);

    boolean existsByTipoMateriaPrima(TipoMateriaPrima tipoMateriaPrima);

    List<Produto> findAllByTipoMateriaPrima(TipoMateriaPrima tipoMateriaPrima);

}
