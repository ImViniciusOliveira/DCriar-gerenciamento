package com.dcriar.exception.custom;

import lombok.Getter;

@Getter
public class CanalVendaNomeDuplicadoException extends RuntimeException {

    private final String nome;

    public CanalVendaNomeDuplicadoException(String nome) {
        super(String.format(
                "Já existe um canal de venda cadastrado com o nome '%s'. Use um nome diferente para continuar.",
                nome
        ));
        this.nome = nome;
    }
}
