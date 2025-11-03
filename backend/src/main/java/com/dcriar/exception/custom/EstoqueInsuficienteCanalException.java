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

    /**
     * O ID do canal de venda onde o estoque é insuficiente.
     */
    private final Long canalVendaId;

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
    public EstoqueInsuficienteCanalException(Long produtoId, Long canalVendaId, int quantidadeRequisitada, int estoqueAtual) {
        super(String.format(
                "Estoque insuficiente no canal. Tentativa de remover %d unidades do produto ID %d no canal ID %d, mas apenas %d unidades estavam disponíveis.",
                Math.abs(quantidadeRequisitada), produtoId, canalVendaId, estoqueAtual
        ));
        this.produtoId = produtoId;
        this.canalVendaId = canalVendaId;
        this.quantidadeRequisitada = quantidadeRequisitada;
        this.estoqueAtual = estoqueAtual;
    }
}
