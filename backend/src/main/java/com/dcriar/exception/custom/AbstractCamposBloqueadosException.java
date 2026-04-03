package com.dcriar.exception.custom;

import lombok.Getter;

import java.util.Map;
import java.util.Set;

/**
 * Base compartilhada para exceções de tentativa de edição em campos estruturais bloqueados.
 */
@Getter
public abstract class AbstractCamposBloqueadosException extends RuntimeException {

    private final Set<String> camposBloqueados;
    private final Map<String, String> motivosBloqueio;

    protected AbstractCamposBloqueadosException(String message, Set<String> camposBloqueados, Map<String, String> motivosBloqueio) {
        super(message);
        this.camposBloqueados = camposBloqueados;
        this.motivosBloqueio = motivosBloqueio;
    }
}
