package com.dcriar.exception.custom;

import lombok.Getter;

/**
 * Exceção lançada quando uma operação tenta acessar uma {@link com.dcriar.domain.production.entity.OrdemDeProducao}
 * que não existe no banco de dados, usando seu ID.
 * <p>
 * É tipicamente capturada por um handler global que retorna uma resposta
 * HTTP 404 (Not Found).
 */
@Getter
public class OrdemDeProducaoNaoEncontradaException extends RuntimeException {

    /**
     * O ID da ordem de produção que não foi encontrada.
     */
    private final Long id;

    /**
     * Constrói a exceção com o ID da ordem de produção não encontrada.
     *
     * @param id O ID utilizado na busca que falhou.
     */
    public OrdemDeProducaoNaoEncontradaException(Long id) {
        super(String.format("Ordem de produção #%d não foi encontrada. Verifique o identificador informado e tente novamente.", id));
        this.id = id;
    }
}
