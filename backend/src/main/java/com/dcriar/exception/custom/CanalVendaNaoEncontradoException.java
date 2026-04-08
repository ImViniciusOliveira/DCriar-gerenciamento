package com.dcriar.exception.custom;

import lombok.Getter;

/**
 * Exceção lançada quando uma operação tenta acessar um Canal de Venda
 * que não existe no banco de dados, usando seu ID.
 * <p>
 * É tipicamente capturada por um handler global que retorna uma resposta
 * HTTP 404 (Not Found).
 */
@Getter
public class CanalVendaNaoEncontradoException extends RuntimeException {

    /**
     * O ID do canal de venda que não foi encontrado.
     */
    private final Long id;

    /**
     * Constrói a exceção com o ID do canal de venda não encontrado.
     *
     * @param id O ID utilizado na busca que falhou.
     */
    public CanalVendaNaoEncontradoException(Long id) {
        super(String.format("Canal de venda #%d não foi encontrado. Verifique o identificador informado e tente novamente.", id));
        this.id = id;
    }
}
