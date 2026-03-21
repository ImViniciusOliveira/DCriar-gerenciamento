package com.dcriar.exception.custom;

import lombok.Getter;

/**
 * Exceção lançada quando a API recebe um tipo de aplicação de preço inválido em um item de venda.
 */
@Getter
public class TipoPrecoAplicadoInvalidoException extends RuntimeException {

    private final String valorInformado;

    public TipoPrecoAplicadoInvalidoException(String valorInformado) {
        super(String.format(
                "O tipo de preço aplicado informado é inválido: '%s'. Valores aceitos: PRECO_PADRAO, PRECO_ALTERADO, DESCONTO_TOTAL.",
                valorInformado
        ));
        this.valorInformado = valorInformado;
    }
}
