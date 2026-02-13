package com.dcriar.exception.custom;

import lombok.Getter;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Exceção lançada ao tentar excluir um {@link com.dcriar.domain.stock.entity.LoteMateriaPrima}
 * que já possui movimentações de saída (consumo, perda, etc.) e, portanto, não pode ser removido.
 * <p>
 * Esta exceção carrega detalhes sobre quais tipos de movimentação impedem a exclusão e suas quantidades.
 */
@Getter
public class ExclusaoLoteBloqueadaException extends RuntimeException {

    /**
     * O ID do lote que não pôde ser excluído.
     */
    private final Long loteId;

    /**
     * Mapa contendo o tipo de movimentação (chave) e a quantidade de ocorrências (valor)
     * que impedem a exclusão.
     */
    private final Map<String, Long> movimentacoesImpeditivas;

    /**
     * Constrói a exceção com os detalhes do bloqueio.
     *
     * @param loteId O ID do lote.
     * @param movimentacoesImpeditivas Mapa com os tipos de movimentação de saída encontrados.
     */
    public ExclusaoLoteBloqueadaException(Long loteId, Map<String, Long> movimentacoesImpeditivas) {
        super(formatMessage(loteId, movimentacoesImpeditivas));
        this.loteId = loteId;
        this.movimentacoesImpeditivas = movimentacoesImpeditivas;
    }

    private static String formatMessage(Long loteId, Map<String, Long> movimentacoes) {
        String detalhes = movimentacoes.entrySet().stream()
                .map(e -> e.getKey() + " (" + e.getValue() + ")")
                .collect(Collectors.joining(", "));
        return String.format("O lote de matéria-prima com ID %d não pode ser excluído pois possui movimentações de saída: %s.", loteId, detalhes);
    }
}
