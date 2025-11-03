package com.dcriar.domain.product.repository;

import com.dcriar.domain.product.entity.Preco;
import com.dcriar.domain.product.entity.Produto;
import com.dcriar.domain.product.entity.enums.TipoPreco;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositório para a entidade Preco.
 * <p>
 * Permite operações de acesso a dados para preços de produtos, incluindo busca por produto e tipo de preço.
 * Não deve conter regras de negócio, apenas consultas e persistência.
 */
@Repository
public interface PrecoRepository extends JpaRepository<Preco, Long> {
    /**
     * Busca o preço de um produto por tipo.
     *
     * @param produto Produto associado
     * @param tipoPreco Tipo de preço
     * @return Preço encontrado, se existir
     */
    Optional<Preco> findByProdutoAndTipoPreco(Produto produto, TipoPreco tipoPreco);

    /**
     * Lista todos os preços de um produto.
     *
     * @param produto Produto associado
     * @return Lista de preços
     */
    List<Preco> findByProduto(Produto produto);
}
