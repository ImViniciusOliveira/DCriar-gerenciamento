package com.dcriar.exception.custom;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class FaixaEstoqueMateriaPrimaInvalidaException extends RuntimeException {

    private final BigDecimal estoqueCritico;
    private final BigDecimal estoqueAceitavel;
    private final String detalhe;

    private FaixaEstoqueMateriaPrimaInvalidaException(
            String detalhe,
            BigDecimal estoqueCritico,
            BigDecimal estoqueAceitavel,
            String message
    ) {
        super(message);
        this.detalhe = detalhe;
        this.estoqueCritico = estoqueCritico;
        this.estoqueAceitavel = estoqueAceitavel;
    }

    public static FaixaEstoqueMateriaPrimaInvalidaException parametrosDevemSerInformadosEmConjunto(
            BigDecimal estoqueCritico,
            BigDecimal estoqueAceitavel
    ) {
        return new FaixaEstoqueMateriaPrimaInvalidaException(
                "faixaEstoque",
                estoqueCritico,
                estoqueAceitavel,
                "Estoque crítico e estoque aceitável devem ser informados em conjunto."
        );
    }

    public static FaixaEstoqueMateriaPrimaInvalidaException aceitavelDeveSerMaiorQueCritico(
            BigDecimal estoqueCritico,
            BigDecimal estoqueAceitavel
    ) {
        return new FaixaEstoqueMateriaPrimaInvalidaException(
                "estoqueAceitavel",
                estoqueCritico,
                estoqueAceitavel,
                "O estoque aceitável deve ser maior que o estoque crítico."
        );
    }
}
