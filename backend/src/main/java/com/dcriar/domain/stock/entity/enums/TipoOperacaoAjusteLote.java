package com.dcriar.domain.stock.entity.enums;

import lombok.Getter;

/**
 * Representa a intenção operacional do usuário ao ajustar um lote.
 */
@Getter
public enum TipoOperacaoAjusteLote {
    AJUSTE("Ajuste"),
    PERDA_DESCARTE("Perda / Descarte");

    private final String descricao;

    TipoOperacaoAjusteLote(String descricao) {
        this.descricao = descricao;
    }
}
