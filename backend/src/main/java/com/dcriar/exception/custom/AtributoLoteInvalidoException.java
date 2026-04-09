package com.dcriar.exception.custom;

import lombok.Getter;

/**
 * Exceção lançada quando um atributo esperado de um lote de matéria-prima
 * é inválido, ausente ou tem um tipo de dado incorreto.
 */
@Getter
public class AtributoLoteInvalidoException extends RuntimeException {

    private final String codigo;
    private final String campo;
    private final String unidadeDescricao;

    /**
     * Constrói a exceção com a mensagem de erro.
     *
     * @param message A mensagem explicando o problema com o atributo do lote.
     */
    private AtributoLoteInvalidoException(String codigo, String campo, String unidadeDescricao, String message) {
        super(message);
        this.codigo = codigo;
        this.campo = campo;
        this.unidadeDescricao = unidadeDescricao;
    }

    public static AtributoLoteInvalidoException larguraMmInvalidaOuAusente() {
        return new AtributoLoteInvalidoException(
                "LARGURA_MM_INVALIDA_OU_AUSENTE",
                "larguraMm",
                null,
                "O atributo 'larguraMm' do lote é inválido ou não existe."
        );
    }
}
