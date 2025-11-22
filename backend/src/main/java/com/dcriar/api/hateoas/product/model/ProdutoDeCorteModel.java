package com.dcriar.api.hateoas.product.model;

import com.dcriar.api.dto.response.product.DimensoesResponseDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProdutoDeCorteModel extends ProdutoModel {

    @Schema(description = "Cor principal do produto.", example = "Marrom")
    private String cor;

    @Schema(description = "Dimensões unitárias do produto.")
    private DimensoesResponseDTO dimensoes;
}
