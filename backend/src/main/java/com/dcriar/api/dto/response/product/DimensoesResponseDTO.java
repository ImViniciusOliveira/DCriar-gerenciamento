package com.dcriar.api.dto.response.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

/**
 * Data Transfer Object (DTO) que representa as dimensões de um produto.
 * <p>
 * Este DTO é usado para comunicar as dimensões físicas (largura e comprimento)
 * de um item, sempre em centímetros.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DimensoesResponseDTO {

    /**
     * A largura unitária do item em centímetros.
     */
    @Schema(description = "Largura unitária do item em centímetros.", example = "20.0")
    private BigDecimal larguraCm;

    /**
     * O comprimento unitário do item em centímetros.
     */
    @Schema(description = "Comprimento unitário do item em centímetros.", example = "30.0")
    private BigDecimal comprimentoCm;
}