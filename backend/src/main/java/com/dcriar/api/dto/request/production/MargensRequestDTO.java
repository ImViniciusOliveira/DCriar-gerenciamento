package com.dcriar.api.dto.request.production;

import com.dcriar.api.validation.annotation.ValidMargensRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

/**
 * Data Transfer Object (DTO) para encapsular as margens de segurança de um corte.
 * <p>
 * Este DTO é um componente reutilizável, embutido em {@link OrdemDeCorteRequestDTO},
 * e é obrigatório quando o modo de cálculo da ordem é 'AUTOMATICO'. As margens
 * (sangria) são adicionadas às dimensões do produto para determinar o tamanho
 * final do corte, garantindo que não haja bordas brancas indesejadas.
 * A validação dos campos é garantida pela anotação {@link ValidMargensRequest}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ValidMargensRequest
public class MargensRequestDTO {

    /**
     * A margem superior a ser adicionada ao corte, em centímetros.
     */
    @Schema(description = "Margem superior a ser adicionada ao corte, em centímetros.", example = "2.0", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal superior;

    /**
     * A margem inferior a ser adicionada ao corte, em centímetros.
     */
    @Schema(description = "Margem inferior a ser adicionada ao corte, em centímetros.", example = "2.0", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal inferior;

    /**
     * A margem esquerda a ser adicionada ao corte, em centímetros.
     */
    @Schema(description = "Margem esquerda a ser adicionada ao corte, em centímetros.", example = "1.5", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal esquerda;

    /**
     * A margem direita a ser adicionada ao corte, em centímetros.
     */
    @Schema(description = "Margem direita a ser adicionada ao corte, em centímetros.", example = "1.5", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal direita;
}
