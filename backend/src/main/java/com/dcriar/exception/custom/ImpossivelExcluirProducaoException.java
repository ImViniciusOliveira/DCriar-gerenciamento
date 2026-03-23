package com.dcriar.exception.custom;

import lombok.Getter;

/**
 * Exceção lançada quando uma tentativa de excluir uma Ordem de Produção é bloqueada
 * por regras de integridade (ex: produtos já vendidos ou retalhos já utilizados).
 * <p>
 * Esta exceção garante que o sistema não permita operações que deixariam o estoque
 * em estado inconsistente ou negativo.
 */
@Getter
public class ImpossivelExcluirProducaoException extends RuntimeException {

    /**
     * Constrói a exceção com a mensagem detalhando o motivo do bloqueio.
     *
     * @param mensagem A descrição do erro e o motivo pelo qual a exclusão não é permitida.
     */
    private ImpossivelExcluirProducaoException(String mensagem) {
        super(mensagem);
    }

    public static ImpossivelExcluirProducaoException estoqueInsuficienteParaEstorno(
            int quantidadeProduzida,
            int saldoAtualProduto
    ) {
        return new ImpossivelExcluirProducaoException(String.format(
                "Estoque insuficiente para estorno. Produzido: %d, Saldo Atual: %d. Produtos já foram vendidos ou consumidos.",
                quantidadeProduzida,
                saldoAtualProduto
        ));
    }

    public static ImpossivelExcluirProducaoException retalhoJaUtilizado(Long loteId, Long ordemDeProducaoOrigemId) {
        return new ImpossivelExcluirProducaoException(
                String.format(
                        "O retalho gerado (Lote #%d), originado pela Ordem de Produção #%d, já foi utilizado em outra produção.",
                        loteId,
                        ordemDeProducaoOrigemId
                )
        );
    }

    public static ImpossivelExcluirProducaoException estoqueCanalInsuficienteParaEstorno(
            Long canalVendaId,
            int quantidadeProduzida,
            int estoqueAtualCanal
    ) {
        return new ImpossivelExcluirProducaoException(String.format(
                "Estoque insuficiente no canal #%d para estorno da produção. Produzido: %d, Estoque no Canal: %d.",
                canalVendaId,
                quantidadeProduzida,
                estoqueAtualCanal
        ));
    }
}
