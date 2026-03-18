package com.dcriar.domain.stock.entity.enums;

import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;

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
    METRO_LINEAR("Metro Linear", "Metros Lineares", "m", true, false, true),
    CENTIMETRO_LINEAR("Centímetro Linear", "Centímetros Lineares", "cm", true, false, true),

    // Unidades de Área
    METRO_QUADRADO("Metro Quadrado", "Metros Quadrados", "m²", true, false, true),
    CENTIMETRO_QUADRADO("Centímetro Quadrado", "Centímetros Quadrados", "cm²", true, false, true),

    // Unidades de Massa
    QUILOGRAMA("Quilograma", "Quilogramas", "kg", false, true, true),
    GRAMA("Grama", "Gramas", "g", false, true, true),

    // Unidades de Volume
    LITRO("Litro", "Litros", "L", false, true, true),
    MILILITRO("Mililitro", "Mililitros", "ml", false, true, true),

    // Unidades de Contagem
    UNIDADE("Unidade", "Unidades", "un", false, true, false), // Para itens não-dimensionais (parafusos, ilhós)
    FOLHA("Folha", "Folhas", "fl", false, true, false),

    // Genérico
    OUTROS("Outros", "Outros", "N/A", false, false, false);

    private final String descricao;
    private final String descricaoPlural;
    private final String simbolo;
    private final boolean permiteCorte;
    private final boolean consumo;
    private final boolean exibirQuantidadeComSimbolo;

    UnidadeDeMedida(String descricao, String descricaoPlural, String simbolo, boolean permiteCorte, boolean consumo, boolean exibirQuantidadeComSimbolo) {
        this.descricao = descricao;
        this.descricaoPlural = descricaoPlural;
        this.simbolo = simbolo;
        this.permiteCorte = permiteCorte;
        this.consumo = consumo;
        this.exibirQuantidadeComSimbolo = exibirQuantidadeComSimbolo;
    }

    public boolean isConsumo() {
        return consumo;
    }

    public UnidadeDeMedida getUnidadeMenorCompativelParaCadastro() {
        return switch (this) {
            case LITRO -> MILILITRO;
            case QUILOGRAMA -> GRAMA;
            case METRO_LINEAR -> CENTIMETRO_LINEAR;
            case METRO_QUADRADO -> CENTIMETRO_QUADRADO;
            default -> null;
        };
    }

    public BigDecimal getFatorConversaoUnidadeMenorParaPrincipal() {
        return switch (this) {
            case LITRO, QUILOGRAMA -> new BigDecimal("1000");
            case METRO_LINEAR -> new BigDecimal("100");
            case METRO_QUADRADO -> new BigDecimal("10000");
            default -> null;
        };
    }

    public boolean exigeLarguraMmNoLote() {
        return permiteCorte;
    }

    public boolean aceitaComoUnidadeDeEstoque(UnidadeDeMedida unidadeInformada) {
        if (unidadeInformada == null) {
            return false;
        }
        return this == unidadeInformada || unidadeInformada == getUnidadeMenorCompativelParaCadastro();
    }

    public boolean aceitaComoCadastroDeConsumo(UnidadeDeMedida unidadeInformada) {
        return aceitaComoUnidadeDeEstoque(unidadeInformada);
    }

    public BigDecimal normalizarQuantidadeDeConsumo(BigDecimal quantidadeInformada, UnidadeDeMedida unidadeInformada) {
        if (quantidadeInformada == null) {
            return null;
        }

        if (unidadeInformada == null || unidadeInformada == this) {
            return quantidadeInformada;
        }

        if (!aceitaComoCadastroDeConsumo(unidadeInformada)) {
            throw new IllegalArgumentException("Unidade incompatível para normalização: " + unidadeInformada);
        }

        return switch (this) {
            case LITRO, QUILOGRAMA -> quantidadeInformada.divide(new BigDecimal("1000"), 4, RoundingMode.HALF_UP);
            case METRO_LINEAR -> quantidadeInformada.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
            case METRO_QUADRADO -> quantidadeInformada.divide(new BigDecimal("10000"), 4, RoundingMode.HALF_UP);
            default -> quantidadeInformada;
        };
    }
}
