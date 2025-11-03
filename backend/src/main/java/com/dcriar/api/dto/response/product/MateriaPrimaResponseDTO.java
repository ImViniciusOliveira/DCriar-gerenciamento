package com.dcriar.api.dto.response.product;

import com.dcriar.domain.stock.entity.enums.UnidadeDeMedida;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * Data Transfer Object (DTO) que representa a resposta de uma Matéria-Prima.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MateriaPrimaResponseDTO {

    /**
     * O ID único da matéria-prima.
     */
    @Schema(description = "ID único da matéria-prima.", example = "1")
    private Long id;

    /**
     * O nome da matéria-prima.
     */
    @Schema(description = "Nome da matéria-prima.", example = "Papel Kraft")
    private String nome;

    /**
     * A unidade de consumo da matéria-prima.
     */
    @Schema(description = "Unidade de consumo da matéria-prima.", example = "FOLHA")
    private UnidadeDeMedida unidadeDeConsumo;
}
