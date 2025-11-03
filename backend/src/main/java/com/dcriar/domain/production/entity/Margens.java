package com.dcriar.domain.production.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * Classe embutível ({@code @Embeddable}) que representa as margens de segurança para um processo de corte.
 * <p>
 * Agrupa as margens superior, inferior, esquerda e direita que podem ser aplicadas
 * a uma ordem de produção para garantir um corte preciso e seguro, desconsiderando as bordas do material.
 */
@Embeddable
@Getter
@Setter
@ToString
@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Margens {

    /**
     * A margem de segurança superior em centímetros.
     */
    @Column(name = "margem_superior_cm", precision = 10, scale = 2)
    private BigDecimal superior;

    /**
     * A margem de segurança inferior em centímetros.
     */
    @Column(name = "margem_inferior_cm", precision = 10, scale = 2)
    private BigDecimal inferior;

    /**
     * A margem de segurança esquerda em centímetros.
     */
    @Column(name = "margem_esquerda_cm", precision = 10, scale = 2)
    private BigDecimal esquerda;

    /**
     * A margem de segurança direita em centímetros.
     */
    @Column(name = "margem_direita_cm", precision = 10, scale = 2)
    private BigDecimal direita;
}
