package com.dcriar.domain.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Classe embutível ({@code @Embeddable}) que representa as dimensões de um item.
 * <p>
 * É usada para agrupar campos relacionados (como largura e comprimento) dentro de
 * uma entidade, mantendo o modelo de domínio organizado e a tabela do banco de dados plana.
 */
@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dimensoes {

    /**
     * Largura do item em centímetros. Valor pode ser nulo se não informado.
     */
    @Column(name = "largura_cm_unitaria", precision = 10, scale = 2)
    private BigDecimal larguraCm;

    /**
     * Comprimento do item em centímetros. Valor pode ser nulo se não informado.
     */
    @Column(name = "comprimento_cm_unitario", precision = 10, scale = 2)
    private BigDecimal comprimentoCm;

}
