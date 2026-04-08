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
    private final String identificadorPublico;

    public LoteCamposBloqueadosException(Long loteId, String identificadorPublico, Set<String> camposBloqueados, Map<String, String> motivosBloqueio) {
        super(String.format(
                "Não é possível alterar os campos estruturais do lote '%s' (%s). Motivos: %s",
                identificadorPublico,
                formatarCampos(camposBloqueados),
                formatarMotivos(motivosBloqueio, camposBloqueados)
        ), camposBloqueados, motivosBloqueio);
        this.loteId = loteId;
        this.identificadorPublico = identificadorPublico;
    }
}
