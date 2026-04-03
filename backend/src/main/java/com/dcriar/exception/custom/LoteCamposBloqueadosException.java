package com.dcriar.exception.custom;

import lombok.Getter;

import java.util.Map;
import java.util.Set;

/**
 * Lançada quando uma atualização tenta alterar campos estruturais de um lote
 * que já possui uso operacional e, por isso, não deve mais ser modificado livremente.
 */
@Getter
public class LoteCamposBloqueadosException extends RuntimeException {

    private final Long loteId;
    private final Set<String> camposBloqueados;
    private final Map<String, String> motivosBloqueio;

    public LoteCamposBloqueadosException(Long loteId, Set<String> camposBloqueados, Map<String, String> motivosBloqueio) {
        super(String.format(
                "O lote com id %d possui campos bloqueados para edição: %s",
                loteId,
                camposBloqueados
        ));
        this.loteId = loteId;
        this.camposBloqueados = camposBloqueados;
        this.motivosBloqueio = motivosBloqueio;
    }
}
