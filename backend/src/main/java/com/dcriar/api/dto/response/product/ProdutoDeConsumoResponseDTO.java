package com.dcriar.api.dto.response.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ProdutoDeConsumoResponseDTO extends ProdutoResponseDTO {

    @Schema(description = "Código do produto fornecido pelo fabricante.", example = "INK-BLK-ES-1L")
    private String codigoFabricante;

    @Schema(description = "Mapa flexível para especificações técnicas.", example = "{\"tipo_tinta\": \"Eco-Solvente\", \"volume_ml\": 1000}")
    private Map<String, String> especificacoes;
}
