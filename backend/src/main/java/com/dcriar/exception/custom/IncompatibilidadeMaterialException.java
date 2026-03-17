package com.dcriar.exception.custom;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class IncompatibilidadeMaterialException extends RuntimeException {
    private IncompatibilidadeMaterialException(String message) {
        super(message);
    }

    public static IncompatibilidadeMaterialException entreProdutoELote(
            String nomeMateriaPrimaProduto,
            String nomeMateriaPrimaLote
    ) {
        return new IncompatibilidadeMaterialException(
                "A matéria-prima do produto (" + nomeMateriaPrimaProduto + ") " +
                        "é diferente da matéria-prima do lote (" + nomeMateriaPrimaLote + ")."
        );
    }
}
