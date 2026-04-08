package com.dcriar.exception.custom;

import lombok.Getter;

import java.util.StringJoiner;
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

    protected static String formatarCampos(Set<String> camposBloqueados) {
        return String.join(", ", camposBloqueados);
    }

    protected static String formatarMotivos(Map<String, String> motivosBloqueio, Set<String> camposBloqueados) {
        StringJoiner joiner = new StringJoiner("; ");

        camposBloqueados.forEach(campo -> {
            String motivo = motivosBloqueio.get(campo);
            if (motivo != null && !motivo.isBlank()) {
                joiner.add(campo + ": " + motivo);
            }
        });

        String resultado = joiner.toString();
        return resultado.isBlank() ? "Revise as regras de bloqueio aplicadas a esse cadastro." : resultado;
    }
}
