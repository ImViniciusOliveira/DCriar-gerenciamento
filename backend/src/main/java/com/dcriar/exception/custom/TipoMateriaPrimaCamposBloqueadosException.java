package com.dcriar.exception.custom;

import lombok.Getter;

import java.util.Map;
import java.util.Set;

/**
 * Lançada quando uma atualização tenta alterar campos estruturais de um tipo de matéria-prima
 * que já possui uso operacional e, por isso, não deve mais ser modificado livremente.
 */
@Getter
public class TipoMateriaPrimaCamposBloqueadosException extends RuntimeException {

    private final Long tipoMateriaPrimaId;
    private final Set<String> camposBloqueados;
    private final Map<String, String> motivosBloqueio;

    public TipoMateriaPrimaCamposBloqueadosException(Long tipoMateriaPrimaId, Set<String> camposBloqueados, Map<String, String> motivosBloqueio) {
        super(String.format(
                "O tipo de matéria-prima com id %d possui campos bloqueados para edição: %s",
                tipoMateriaPrimaId,
                camposBloqueados
        ));
        this.tipoMateriaPrimaId = tipoMateriaPrimaId;
        this.camposBloqueados = camposBloqueados;
        this.motivosBloqueio = motivosBloqueio;
    }
}
