package com.dcriar.exception.custom;

import lombok.Getter;

import java.util.Map;
import java.util.Set;

/**
 * Lançada quando uma atualização tenta alterar campos estruturais de um produto
 * que já possui uso operacional e, portanto, não deve mais ter esses dados modificados.
 */
@Getter
public class ProdutoCamposBloqueadosException extends RuntimeException {

    private final Long produtoId;
    private final Set<String> camposBloqueados;
    private final Map<String, String> motivosBloqueio;

    public ProdutoCamposBloqueadosException(Long produtoId, Set<String> camposBloqueados, Map<String, String> motivosBloqueio) {
        super(String.format(
                "O produto com id %d possui campos bloqueados para edição: %s",
                produtoId,
                camposBloqueados
        ));
        this.produtoId = produtoId;
        this.camposBloqueados = camposBloqueados;
        this.motivosBloqueio = motivosBloqueio;
    }
}
