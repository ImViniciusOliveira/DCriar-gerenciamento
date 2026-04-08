package com.dcriar.exception.custom;

import lombok.Getter;

import java.util.Map;
import java.util.Set;

/**
 * Lançada quando uma atualização tenta alterar campos estruturais de um tipo de matéria-prima
 * que já possui uso operacional e, por isso, não deve mais ser modificado livremente.
 */
@Getter
public class TipoMateriaPrimaCamposBloqueadosException extends AbstractCamposBloqueadosException {

    private final Long tipoMateriaPrimaId;

    public TipoMateriaPrimaCamposBloqueadosException(Long tipoMateriaPrimaId, Set<String> camposBloqueados, Map<String, String> motivosBloqueio) {
        super(String.format(
                "Não é possível alterar os campos estruturais do tipo de matéria-prima #%d (%s). Motivos: %s",
                tipoMateriaPrimaId,
                formatarCampos(camposBloqueados),
                formatarMotivos(motivosBloqueio, camposBloqueados)
        ), camposBloqueados, motivosBloqueio);
        this.tipoMateriaPrimaId = tipoMateriaPrimaId;
    }
}
