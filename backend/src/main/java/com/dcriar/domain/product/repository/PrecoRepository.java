package com.dcriar.domain.product.repository;

import com.dcriar.domain.product.entity.Preco;
import com.dcriar.domain.product.entity.Produto;
import com.dcriar.domain.product.entity.enums.TipoPreco;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * Repositório para a entidade Preco.
 * <p>
 * Permite operações de acesso a dados para preços de produtos, incluindo busca por produto e tipo de preço.
 * Não deve conter regras de negócio, apenas consultas e persistência.
 */
@Repository
public interface PrecoRepository extends JpaRepository<Preco, Long> {

    /**
     * Lista todos os preços de um produto.
     *
     * @param produto Produto associado
     * @return Lista de preços
     */
    List<Preco> findByProduto(Produto produto);

    /**
     * Busca os preços de uma coleção de produtos para um tipo de preço específico.
     *
     * @param produtos Coleção de produtos
     * @param tipoPreco Tipo de preço
     * @return Lista de preços encontrados
     */
    List<Preco> findByProdutoInAndTipoPreco(Collection<Produto> produtos, TipoPreco tipoPreco);
}
