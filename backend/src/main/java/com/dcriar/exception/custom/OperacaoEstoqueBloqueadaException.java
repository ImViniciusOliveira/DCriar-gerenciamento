package com.dcriar.exception.custom;

import lombok.Getter;

import java.util.Map;
import java.util.Set;

/**
 * Exceção lançada quando uma operação é bloqueada por estado inconsistente do estoque.
 */
@Getter
public class OperacaoEstoqueBloqueadaException extends AbstractCamposBloqueadosException {

    private final String tipoRecurso;
    private final Long recursoId;
    private final String identificador;

    public OperacaoEstoqueBloqueadaException(
            String tipoRecurso,
            Long recursoId,
            String identificador,
            Set<String> camposBloqueados,
            Map<String, String> motivosBloqueio
    ) {
        super(
                "Operacao bloqueada para " + tipoRecurso.toLowerCase() + " '" + identificador + "'. "
                        + formatarMotivos(motivosBloqueio, camposBloqueados),
                camposBloqueados,
                motivosBloqueio
        );
        this.tipoRecurso = tipoRecurso;
        this.recursoId = recursoId;
        this.identificador = identificador;
    }
}
