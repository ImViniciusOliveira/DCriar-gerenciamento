package com.dcriar.exception.custom;

import com.dcriar.domain.common.util.HumanNumberDisplayFormatter;
import lombok.Getter;

import java.math.BigDecimal;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
@Getter
public class QuantidadeExcedeCapacidadeLoteException extends RuntimeException {
    private static final BigDecimal COMPRIMENTO_INDISPONIVEL = BigDecimal.valueOf(Long.MAX_VALUE);

    private final int quantidadeSolicitada;
    private final BigDecimal comprimentoNecessarioNormalCm;
    private final BigDecimal comprimentoNecessarioRotacionadoCm;
    private final BigDecimal comprimentoDisponivelLoteCm;

    private QuantidadeExcedeCapacidadeLoteException(
            int quantidadeSolicitada,
            BigDecimal comprimentoNecessarioNormalCm,
            BigDecimal comprimentoNecessarioRotacionadoCm,
            BigDecimal comprimentoDisponivelLoteCm,
            String message
    ) {
        super(message);
        this.quantidadeSolicitada = quantidadeSolicitada;
        this.comprimentoNecessarioNormalCm = comprimentoNecessarioNormalCm;
        this.comprimentoNecessarioRotacionadoCm = comprimentoNecessarioRotacionadoCm;
        this.comprimentoDisponivelLoteCm = comprimentoDisponivelLoteCm;
    }

    public static QuantidadeExcedeCapacidadeLoteException ambasOrientacoes(
            int quantidadeSolicitada,
            BigDecimal comprimentoNecessarioNormalCm,
            BigDecimal comprimentoNecessarioRotacionadoCm,
            BigDecimal comprimentoDisponivelLoteCm
    ) {
        return new QuantidadeExcedeCapacidadeLoteException(
                quantidadeSolicitada,
                comprimentoNecessarioNormalCm,
                comprimentoNecessarioRotacionadoCm,
                comprimentoDisponivelLoteCm,
                String.format(
                        "A quantidade solicitada (%s) excede a capacidade do lote em ambas as orientações. " +
                                "Comprimento necessário no layout normal: %s, no layout rotacionado: %s. " +
                                "Comprimento disponível no lote: %s.",
                        HumanNumberDisplayFormatter.formatInteger(quantidadeSolicitada),
                        formatarComprimento(comprimentoNecessarioNormalCm),
                        formatarComprimento(comprimentoNecessarioRotacionadoCm),
                        HumanNumberDisplayFormatter.formatLengthCm(comprimentoDisponivelLoteCm)
                )
        );
    }

    private static String formatarComprimento(BigDecimal comprimentoCm) {
        if (comprimentoCm == null || comprimentoCm.compareTo(COMPRIMENTO_INDISPONIVEL) >= 0) {
            return "não aplicável";
        }
        return HumanNumberDisplayFormatter.formatLengthCm(comprimentoCm);
    }
}
