package com.dcriar.domain.stock.entity.enums;

import lombok.Getter;

/**
 * Enum para padronizar as unidades de medida utilizadas no sistema.
 * <p>
 * Garante a consistência dos dados e evita erros de digitação,
 * além de centralizar as unidades de medida aceitas pela aplicação.
 * <p>
 * Implementa o Strategy Pattern para definir os comportamentos de cada unidade.
 */
@Getter
public enum UnidadeDeMedida {

    // Unidades de Comprimento
    METRO_LINEAR("Metro Linear", "m", true, false),
    CENTIMETRO_LINEAR("Centímetro Linear", "cm", true, false),

    // Unidades de Área
    METRO_QUADRADO("Metro Quadrado", "m²", true, false),
    CENTIMETRO_QUADRADO("Centímetro Quadrado", "cm²", true, false),

    // Unidades de Massa
    QUILOGRAMA("Quilograma", "kg", false, true),
    GRAMA("Grama", "g", false, true),

    // Unidades de Volume
    LITRO("Litro", "L", false, true),
    MILILITRO("Mililitro", "ml", false, true),

    // Unidades de Contagem
    UNIDADE("Unidade", "un", false, true),

    // Genérico
    OUTROS("Outros", "N/A", false, false);

    private final String descricao;
    private final String simbolo;
    private final boolean permiteCorte;
    private final boolean consumoDireto;

    UnidadeDeMedida(String descricao, String simbolo, boolean permiteCorte, boolean consumoDireto) {
        this.descricao = descricao;
        this.simbolo = simbolo;
        this.permiteCorte = permiteCorte;
        this.consumoDireto = consumoDireto;
    }
}
