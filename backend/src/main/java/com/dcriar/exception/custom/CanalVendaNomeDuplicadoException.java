package com.dcriar.exception.custom;

import lombok.Getter;

@Getter
public class CanalVendaNomeDuplicadoException extends RuntimeException {

    private final String nome;

    public CanalVendaNomeDuplicadoException(String nome) {
        super(String.format("Canal de venda já existente: %s", nome));
        this.nome = nome;
    }
}
