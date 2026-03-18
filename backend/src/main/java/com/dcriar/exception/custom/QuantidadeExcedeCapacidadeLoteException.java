package com.dcriar.exception.custom;

import java.math.BigDecimal;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class QuantidadeExcedeCapacidadeLoteException extends RuntimeException {
    private static final BigDecimal COMPRIMENTO_INDISPONIVEL = BigDecimal.valueOf(Long.MAX_VALUE);

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
                                "Comprimento necessário no layout normal: %s, no layout rotacionado: %s. " +
                                "Comprimento disponível no lote: %.2f cm.",
                        quantidadeSolicitada,
                        formatarComprimento(comprimentoNecessarioNormalCm),
                        formatarComprimento(comprimentoNecessarioRotacionadoCm),
                        comprimentoDisponivelLoteCm
                )
        );
    }

    private static String formatarComprimento(BigDecimal comprimentoCm) {
        if (comprimentoCm == null || comprimentoCm.compareTo(COMPRIMENTO_INDISPONIVEL) >= 0) {
            return "não aplicável";
        }
        return String.format("%.2f cm", comprimentoCm);
    }
}
