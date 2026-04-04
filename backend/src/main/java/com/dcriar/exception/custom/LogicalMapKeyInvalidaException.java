package com.dcriar.exception.custom;

import lombok.Getter;

@Getter
public class LogicalMapKeyInvalidaException extends RuntimeException {

    private final String fieldPath;

    private LogicalMapKeyInvalidaException(String fieldPath, String message) {
        super(message);
        this.fieldPath = fieldPath;
    }

    public static LogicalMapKeyInvalidaException chaveVazia(String fieldPath) {
        return new LogicalMapKeyInvalidaException(
                fieldPath,
                "As chaves do campo '" + fieldPath + "' nao podem ser vazias."
        );
    }

    public static LogicalMapKeyInvalidaException chavesEquivalentes(String fieldPath, String primeiraChave, String segundaChave) {
        return new LogicalMapKeyInvalidaException(
                fieldPath,
                "As chaves '" + primeiraChave + "' e '" + segundaChave + "' sao equivalentes e nao podem coexistir."
        );
    }
}
