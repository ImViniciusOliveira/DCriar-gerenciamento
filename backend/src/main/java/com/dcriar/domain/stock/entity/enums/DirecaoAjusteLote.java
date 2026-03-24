package com.dcriar.domain.stock.entity.enums;

import lombok.Getter;

/**
 * Representa a direção do ajuste manual de lote.
 */
@Getter
public enum DirecaoAjusteLote {
    ADICIONAR("Adicionar"),
    RETIRAR("Retirar");

    private final String descricao;

    DirecaoAjusteLote(String descricao) {
        this.descricao = descricao;
    }
}
