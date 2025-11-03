package com.dcriar.api.dto.response.production;

import com.dcriar.domain.stock.entity.enums.UnidadeDeMedida;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Data Transfer Object (DTO) para a resposta da simulação de uma produção por consumo direto.
 * <p>
 * Este DTO informa a quantidade total de matéria-prima que seria necessária para produzir
 * uma determinada quantidade de um produto, sem efetivamente movimentar o estoque.
 */
@Getter
@Setter
@Builder
public class SimulacaoConsumoDiretoResponseDTO {

    @Schema(description = "Consumo total estimado de matéria-prima, na unidade de consumo padrão do material.", example = "10.0")
    private BigDecimal consumoTotalEstimado;

    @Schema(description = "Unidade de medida do consumo estimado.", example = "LITRO")
    private UnidadeDeMedida unidadeDeConsumo;
}
