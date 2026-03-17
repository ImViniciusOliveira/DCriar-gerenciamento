package com.dcriar.exception.custom;

import lombok.Getter;

/**
 * Exceção lançada quando um atributo esperado de um lote de matéria-prima
 * é inválido, ausente ou tem um tipo de dado incorreto.
 */
@Getter
public class AtributoLoteInvalidoException extends RuntimeException {

    /**
     * Constrói a exceção com a mensagem de erro.
     *
     * @param message A mensagem explicando o problema com o atributo do lote.
     */
    private AtributoLoteInvalidoException(String message) {
        super(message);
    }

    public static AtributoLoteInvalidoException larguraMmObrigatoriaParaCalculoCusto(String unidadeDescricao) {
        return new AtributoLoteInvalidoException(String.format(
                "Para lotes em %s, o atributo 'larguraMm' é obrigatório e deve ser um número para o cálculo de custo.",
                unidadeDescricao
        ));
    }

    public static AtributoLoteInvalidoException larguraMmInvalidaOuAusente() {
        return new AtributoLoteInvalidoException("O atributo 'larguraMm' do lote é inválido ou não existe.");
    }
}
