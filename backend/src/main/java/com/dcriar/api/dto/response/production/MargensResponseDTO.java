package com.dcriar.api.dto.response.production;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.math.BigDecimal;

/**
 * DTO de resposta para margens de segurança de corte.
 * Utilizado para retornar as margens aplicadas em processos de produção.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MargensResponseDTO {
    /** Margem superior aplicada ao corte, em centímetros. */
    @Schema(description = "Margem superior aplicada ao corte, em centímetros.", example = "2.0")
    private BigDecimal superior;

    /** Margem inferior aplicada ao corte, em centímetros. */
    @Schema(description = "Margem inferior aplicada ao corte, em centímetros.", example = "2.0")
    private BigDecimal inferior;

    /** Margem esquerda aplicada ao corte, em centímetros. */
    @Schema(description = "Margem esquerda aplicada ao corte, em centímetros.", example = "1.5")
    private BigDecimal esquerda;

    /** Margem direita aplicada ao corte, em centímetros. */
    @Schema(description = "Margem direita aplicada ao corte, em centímetros.", example = "1.5")
    private BigDecimal direita;
}

