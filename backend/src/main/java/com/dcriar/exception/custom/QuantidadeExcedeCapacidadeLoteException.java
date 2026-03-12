package com.dcriar.exception.custom;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class QuantidadeExcedeCapacidadeLoteException extends RuntimeException {
    public QuantidadeExcedeCapacidadeLoteException(String message) {
        super(message);
    }
}
