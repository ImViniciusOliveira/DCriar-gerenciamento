package com.dcriar.exception.custom;

import java.math.BigDecimal;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class QuantidadeExcedeCapacidadeLoteException extends RuntimeException {
    private QuantidadeExcedeCapacidadeLoteException(String message) {
        super(message);
    }

    public static QuantidadeExcedeCapacidadeLoteException ambasOrientacoes(
            int quantidadeSolicitada,
            BigDecimal comprimentoNecessarioNormalCm,
            BigDecimal comprimentoNecessarioRotacionadoCm,
            BigDecimal comprimentoDisponivelLoteCm
    ) {
        return new QuantidadeExcedeCapacidadeLoteException(
                String.format(
                        "A quantidade solicitada (%d) excede a capacidade do lote em ambas as orientações. " +
                                "Comprimento necessário no layout normal: %.2f cm, no layout rotacionado: %.2f cm. " +
                                "Comprimento disponível no lote: %.2f cm.",
                        quantidadeSolicitada,
                        comprimentoNecessarioNormalCm,
                        comprimentoNecessarioRotacionadoCm,
                        comprimentoDisponivelLoteCm
                )
        );
    }
}
