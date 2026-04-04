package com.dcriar.exception.custom;

import lombok.Getter;

@Getter
public class LogicalMapKeyInvalidaException extends RuntimeException {

    private final String fieldPath;

    public LogicalMapKeyInvalidaException(String fieldPath, String message) {
        super(message);
        this.fieldPath = fieldPath;
    }
}
