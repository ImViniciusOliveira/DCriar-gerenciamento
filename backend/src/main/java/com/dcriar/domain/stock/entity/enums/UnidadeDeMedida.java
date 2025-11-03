package com.dcriar.domain.stock.entity.enums;

import lombok.Getter;

/**
 * Enum para padronizar as unidades de medida utilizadas no sistema.
 * <p>
 * Garante a consistência dos dados e evita erros de digitação,
 * além de centralizar as unidades de medida aceitas pela aplicação.
 */
@Getter
public enum UnidadeDeMedida {

    // Unidades de Comprimento
    METRO_LINEAR("Metro Linear", "m"),
    CENTIMETRO_LINEAR("Centímetro Linear", "cm"),

    // Unidades de Área
    METRO_QUADRADO("Metro Quadrado", "m²"),
    CENTIMETRO_QUADRADO("Centímetro Quadrado", "cm²"),

    // Unidades de Massa
    QUILOGRAMA("Quilograma", "kg"),
    GRAMA("Grama", "g"),

    // Unidades de Volume
    LITRO("Litro", "L"),
    MILILITRO("Mililitro", "ml"),

    // Unidades de Contagem
    UNIDADE("Unidade", "un"),

    // Genérico
    OUTROS("Outros", "N/A");

    private final String descricao;
    private final String simbolo;

    UnidadeDeMedida(String descricao, String simbolo) {
        this.descricao = descricao;
        this.simbolo = simbolo;
    }
}

