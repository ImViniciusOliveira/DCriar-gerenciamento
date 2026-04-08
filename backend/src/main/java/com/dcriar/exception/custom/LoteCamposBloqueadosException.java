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
                "Não é possível alterar os campos estruturais do lote #%d (%s). Motivos: %s",
                loteId,
                formatarCampos(camposBloqueados),
                formatarMotivos(motivosBloqueio, camposBloqueados)
        ), camposBloqueados, motivosBloqueio);
        this.loteId = loteId;
    }
}
