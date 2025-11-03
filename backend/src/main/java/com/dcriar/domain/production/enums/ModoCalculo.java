package com.dcriar.domain.production.enums;

/**
 * Enum que define o modo de cálculo para uma ordem de produção.
 */
public enum ModoCalculo {
    /**
     * O sistema calcula o tamanho final do corte com base nas dimensões unitárias
     * do produto, na quantidade e nas margens fornecidas.
     */
    AUTOMATICO,

    /**
     * O operador ignora o cálculo automático e fornece diretamente as dimensões
     * finais do material que foi consumido.
     */
    MANUAL
}
