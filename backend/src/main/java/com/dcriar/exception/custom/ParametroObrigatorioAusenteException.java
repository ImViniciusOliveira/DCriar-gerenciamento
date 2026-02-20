package com.dcriar.exception.custom;

import lombok.Getter;

/**
 * Exceção lançada quando um parâmetro obrigatório de busca não é fornecido.
 * <p>
 * Determinados endpoints de busca requerem parâmetros obrigatórios para funcionar.
 * Quando um desses parâmetros está ausente ou nulo, esta exceção é lançada.
 * <p>
 * É tipicamente capturada por um handler global que retorna uma resposta
 * HTTP 400 (Bad Request).
 */
@Getter
public class ParametroObrigatorioAusenteException extends RuntimeException {

    /**
     * O nome do parâmetro obrigatório que foi não fornecido.
     */
    private final String nomParametro;

    /**
     * Constrói a exceção com o nome do parâmetro ausente.
     *
     * @param nomParametro O nome do parâmetro obrigatório que está faltando.
     */
    public ParametroObrigatorioAusenteException(String nomParametro) {
        super(String.format(
                "Parâmetro obrigatório ausente: '%s'. Este parâmetro é necessário para realizar a operação.",
                nomParametro
        ));
        this.nomParametro = nomParametro;
    }
}


