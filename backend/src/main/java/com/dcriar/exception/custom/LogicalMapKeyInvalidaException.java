package com.dcriar.exception.custom;

import lombok.Getter;

@Getter
public class LogicalMapKeyInvalidaException extends RuntimeException {

    private final String codigo;
    private final String fieldPath;
    private final String primeiraChave;
    private final String segundaChave;

    private LogicalMapKeyInvalidaException(
            String codigo,
            String fieldPath,
            String primeiraChave,
            String segundaChave,
            String message
    ) {
        super(message);
        this.codigo = codigo;
        this.fieldPath = fieldPath;
        this.primeiraChave = primeiraChave;
        this.segundaChave = segundaChave;
    }

    public static LogicalMapKeyInvalidaException chaveVazia(String fieldPath) {
        return new LogicalMapKeyInvalidaException(
                "CHAVE_VAZIA",
                fieldPath,
                null,
                null,
                "As chaves do campo '" + fieldPath + "' nao podem ser vazias."
        );
    }

    public static LogicalMapKeyInvalidaException chavesEquivalentes(String fieldPath, String primeiraChave, String segundaChave) {
        return new LogicalMapKeyInvalidaException(
                "CHAVES_EQUIVALENTES",
                fieldPath,
                primeiraChave,
                segundaChave,
                "As chaves '" + primeiraChave + "' e '" + segundaChave + "' sao equivalentes e nao podem coexistir."
        );
    }
}
