package com.dcriar.exception.custom;

import lombok.Getter;

import java.time.LocalDate;

/**
 * Exceção lançada quando o período informado para análise de vendas é inválido.
 * <p>
 * Cobre os casos de data inicial posterior à final e de intervalo superior ao limite de 366 dias.
 * Datas ausentes ou com formato inválido são tratadas pelo handler de parâmetros do Spring.
 */
@Getter
public class PeriodoAnaliseInvalidoException extends RuntimeException {

    private final String codigo;
    private final String dataInicio;
    private final String dataFim;
    private final Long diasInformados;

    private PeriodoAnaliseInvalidoException(String codigo, String mensagem, String dataInicio, String dataFim, Long diasInformados) {
        super(mensagem);
        this.codigo = codigo;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
        this.diasInformados = diasInformados;
    }

    public static PeriodoAnaliseInvalidoException periodoInvertido(LocalDate dataInicio, LocalDate dataFim) {
        return new PeriodoAnaliseInvalidoException(
                "PERIODO_INVERTIDO",
                "A data inicial deve ser anterior ou igual à data final.",
                dataInicio.toString(),
                dataFim.toString(),
                null
        );
    }

    public static PeriodoAnaliseInvalidoException periodoExcedeLimite(LocalDate dataInicio, LocalDate dataFim, long dias) {
        return new PeriodoAnaliseInvalidoException(
                "PERIODO_EXCEDE_LIMITE",
                String.format("O período informado de %d dias excede o limite máximo de 366 dias.", dias),
                dataInicio.toString(),
                dataFim.toString(),
                dias
        );
    }
}
