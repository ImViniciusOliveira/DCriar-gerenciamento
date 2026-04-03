package com.dcriar.exception.custom;

import lombok.Getter;

import java.util.Map;
import java.util.Set;

/**
 * Lançada quando uma atualização tenta alterar campos estruturais de um lote
 * que já possui uso operacional e, por isso, não deve mais ser modificado livremente.
 */
@Getter
public class LoteCamposBloqueadosException extends AbstractCamposBloqueadosException {

    private final Long loteId;

    public LoteCamposBloqueadosException(Long loteId, Set<String> camposBloqueados, Map<String, String> motivosBloqueio) {
        super(String.format(
                "O lote com id %d possui campos bloqueados para edição: %s",
                loteId,
                camposBloqueados
        ), camposBloqueados, motivosBloqueio);
        this.loteId = loteId;
    }
}
