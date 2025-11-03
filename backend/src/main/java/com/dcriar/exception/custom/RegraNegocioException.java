package com.dcriar.exception.custom;

/**
 * Exceção genérica para violações de regras de negócio.
 * <p>
 * Esta exceção deve ser usada para representar erros de fluxo de negócio que
 * não se encaixam em categorias mais específicas. Por exemplo, tentar realizar
 * uma ação em um estado inválido do sistema.
 * <p>
 * Ela é tipicamente capturada por um handler global que retorna uma resposta
 * HTTP 400 (Bad Request) ou 409 (Conflict), dependendo do contexto.
 */
public class RegraNegocioException extends RuntimeException {

    /**
     * Constrói a exceção com a mensagem que descreve a regra de negócio violada.
     *
     * @param mensagem A descrição do erro.
     */
    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
