package com.dcriar.domain.common.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Estrutura compartilhada para expor e validar campos bloqueados para edição.
 */
public record CamposBloqueadosInfo(
        Set<String> camposBloqueados,
        Map<String, String> motivosBloqueio
) {

    public CamposBloqueadosInfo {
        camposBloqueados = Collections.unmodifiableSet(new LinkedHashSet<>(camposBloqueados));
        motivosBloqueio = Collections.unmodifiableMap(new LinkedHashMap<>(motivosBloqueio));

        if (!camposBloqueados.equals(motivosBloqueio.keySet())) {
            throw new IllegalArgumentException("Campos bloqueados e motivos de bloqueio devem possuir as mesmas chaves.");
        }
    }

    public static CamposBloqueadosInfo vazio() {
        return new CamposBloqueadosInfo(Collections.emptySet(), Collections.emptyMap());
    }

    public static CamposBloqueadosInfo fromMotivos(Map<String, String> motivosBloqueio) {
        return new CamposBloqueadosInfo(motivosBloqueio.keySet(), motivosBloqueio);
    }

    public boolean estaVazio() {
        return camposBloqueados.isEmpty();
    }

    public boolean contemCampo(String campo) {
        return motivosBloqueio.containsKey(campo);
    }
}
