package com.dcriar.api.hateoas.product.model;

import com.dcriar.domain.stock.entity.enums.UnidadeDeMedida;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProdutoDeConsumoModel extends ProdutoModel {

    @Schema(description = "Código do produto fornecido pelo fabricante.", example = "INK-BLK-ES-1L")
    private String codigoFabricante;

    @Schema(description = "Unidade escolhida pelo usuário para o cadastro do consumo por produto.", example = "MILILITRO")
    private UnidadeDeMedida unidadeCadastroConsumo;

    @Schema(description = "Mapa flexível para especificações técnicas.", example = "{\"tipo_tinta\": \"Eco-Solvente\", \"volume_ml\": 1000}")
    private Map<String, String> especificacoes;
}
