package com.dcriar.exception.custom;

import lombok.Getter;

import java.util.Set;

/**
 * Exceção lançada ao tentar excluir um {@link com.dcriar.domain.product.entity.Produto}
 * que ainda está em uso por outras entidades do sistema (ex: Ordens de Corte).
 * <p>
 * Esta exceção carrega os IDs das entidades que referenciam o produto,
 * permitindo que o handler de exceções retorne uma mensagem de erro detalhada
 * para o cliente da API.
 */
@Getter
public class ProdutoEmUsoException extends RuntimeException {

    /**
     * O ID do produto que não pôde ser excluído.
     */
    private final Long produtoId;
    private final String nomeProduto;

    /**
     * Um conjunto de IDs das entidades que estão utilizando o produto.
     */
    private final Set<Long> entidadeIds;
    private final Set<String> entidadeLabels;

    /**
     * Constrói a exceção com os detalhes da violação.
     *
     * @param produtoId O ID do produto que se tentou excluir.
     * @param entidadeIds O conjunto de IDs das entidades que impedem a exclusão.
     */
    public ProdutoEmUsoException(Long produtoId, String nomeProduto, Set<Long> entidadeIds, Set<String> entidadeLabels) {
        super(String.format(
                "Não é possível excluir o produto '%s' porque ele ainda está em uso em %s. Remova ou ajuste esses vínculos antes de tentar excluir o produto.",
                nomeProduto,
                descreverQuantidade(entidadeIds.size(), "ordem de produção", "ordens de produção")
        ));
        this.produtoId = produtoId;
        this.nomeProduto = nomeProduto;
        this.entidadeIds = entidadeIds;
        this.entidadeLabels = entidadeLabels;
    }

    private static String descreverQuantidade(int quantidade, String singular, String plural) {
        return quantidade + " " + (quantidade == 1 ? singular : plural);
    }
}
