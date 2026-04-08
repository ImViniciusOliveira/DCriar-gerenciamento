package com.dcriar.exception.custom;

import lombok.Getter;

/**
 * Exceção lançada quando uma movimentação de venda ou ajuste de estoque não pode ser
 * concluída por falta de unidades disponíveis em um Canal de Venda específico.
 * <p>
 * Esta exceção carrega um contexto detalhado sobre a falha, permitindo a criação
 * de mensagens de erro claras e informativas.
 */
@Getter
public class EstoqueInsuficienteCanalException extends RuntimeException {

    /**
     * O ID do produto com estoque insuficiente.
     */
    private final Long produtoId;
    private final String produtoLabel;

    /**
     * O ID do canal de venda onde o estoque é insuficiente.
     */
    private final Long canalVendaId;
    private final String nomeCanalVenda;

    /**
     * A quantidade de unidades que a operação tentou movimentar.
     */
    private final Integer quantidadeRequisitada;

    /**
     * A quantidade de unidades que estava realmente disponível no momento da falha.
     */
    private final Integer estoqueAtual;

    /**
     * Constrói a exceção com todos os detalhes da falha de estoque no canal.
     *
     * @param produtoId O ID do produto.
     * @param canalVendaId O ID do canal de venda.
     * @param quantidadeRequisitada A quantidade que se tentou remover.
     * @param estoqueAtual A quantidade que estava disponível no momento da falha.
     */
    public EstoqueInsuficienteCanalException(
            Long produtoId,
            String produtoLabel,
            Long canalVendaId,
            String nomeCanalVenda,
            int quantidadeRequisitada,
            int estoqueAtual
    ) {
        super(String.format(
                "Não é possível remover %d %s do produto '%s' no canal de venda '%s' porque há apenas %d %s disponíveis nesse canal.",
                Math.abs(quantidadeRequisitada),
                descreverUnidade(Math.abs(quantidadeRequisitada)),
                produtoLabel,
                nomeCanalVenda,
                estoqueAtual,
                descreverUnidade(estoqueAtual)
        ));
        this.produtoId = produtoId;
        this.produtoLabel = produtoLabel;
        this.canalVendaId = canalVendaId;
        this.nomeCanalVenda = nomeCanalVenda;
        this.quantidadeRequisitada = quantidadeRequisitada;
        this.estoqueAtual = estoqueAtual;
    }

    private static String descreverUnidade(int quantidade) {
        return quantidade == 1 ? "unidade" : "unidades";
    }
}
