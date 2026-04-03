package com.dcriar.domain.common.util;

import com.dcriar.domain.common.model.CamposBloqueadosInfo;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Utilitário para reduzir duplicação na montagem e validação de bloqueios por campo.
 */
public final class CamposBloqueadosUtils {

    private CamposBloqueadosUtils() {
    }

    public static CamposBloqueadosInfo bloquearTodos(Set<String> camposSensiveis, String motivoPadrao) {
        if (camposSensiveis == null || camposSensiveis.isEmpty()) {
            return CamposBloqueadosInfo.vazio();
        }

        Map<String, String> bloqueios = new LinkedHashMap<>();
        camposSensiveis.forEach(campo -> bloqueios.put(campo, motivoPadrao));
        return CamposBloqueadosInfo.fromMotivos(bloqueios);
    }

    public static Set<String> intersectarTentativas(CamposBloqueadosInfo camposBloqueadosInfo, Stream<String> camposTentados) {
        return camposTentados
                .filter(camposBloqueadosInfo::contemCampo)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }
}
