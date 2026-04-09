package com.dcriar.exception.custom;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
@Getter
public class IncompatibilidadeMaterialException extends RuntimeException {
    private final String nomeMateriaPrimaProduto;
    private final String nomeMateriaPrimaLote;

    private IncompatibilidadeMaterialException(String nomeMateriaPrimaProduto, String nomeMateriaPrimaLote, String message) {
        super(message);
        this.nomeMateriaPrimaProduto = nomeMateriaPrimaProduto;
        this.nomeMateriaPrimaLote = nomeMateriaPrimaLote;
    }

    public static IncompatibilidadeMaterialException entreProdutoELote(
            String nomeMateriaPrimaProduto,
            String nomeMateriaPrimaLote
    ) {
        return new IncompatibilidadeMaterialException(
                nomeMateriaPrimaProduto,
                nomeMateriaPrimaLote,
                "A matéria-prima do produto (" + nomeMateriaPrimaProduto + ") " +
                        "é diferente da matéria-prima do lote (" + nomeMateriaPrimaLote + ")."
        );
    }
}
