package com.dcriar.domain.product.repository;

import com.dcriar.domain.product.entity.Preco;
import com.dcriar.domain.product.entity.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * Repositório para o preço comercial dos produtos.
 */
@Repository
public interface PrecoRepository extends JpaRepository<Preco, Long> {

    /**
     * Busca o preço comercial de um produto específico.
     *
     * @param produto Produto associado ao preço
     * @return Preço comercial encontrado ou {@code null} quando ainda não existe cadastro
     */
    Preco findFirstByProduto(Produto produto);

    /**
     * Busca os preços comerciais de uma coleção de produtos para enriquecimento em lote.
     *
     * @param produtos Produtos a consultar
     * @return Lista dos preços encontrados
     */
    List<Preco> findByProdutoIn(Collection<Produto> produtos);
}
