package com.dcriar.api.dto.request.product;

import com.dcriar.api.validation.annotation.ValidDimensoesRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

/**
 * Data Transfer Object (DTO) para encapsular as dimensões de um item.
 * <p>
 * Este DTO é um componente reutilizável, embutido em outros DTOs de requisição
 * (como {@link ProdutoRequestDTO}) para representar medidas de largura e comprimento.
 * A validação dos campos é garantida pela anotação {@link ValidDimensoesRequest}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ValidDimensoesRequest
public class DimensoesRequestDTO {

    /**
     * A medida da largura do item, em centímetros.
     */
    @Schema(description = "A medida da largura do item, em centímetros.", example = "20.0", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal larguraCm;

    /**
     * A medida do comprimento do item, em centímetros.
     */
    @Schema(description = "A medida do comprimento do item, em centímetros.", example = "30.0", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal comprimentoCm;
}
