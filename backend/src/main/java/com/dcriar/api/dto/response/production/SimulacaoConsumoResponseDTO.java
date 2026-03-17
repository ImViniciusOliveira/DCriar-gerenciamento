package com.dcriar.api.dto.response.production;

import com.dcriar.domain.stock.entity.enums.UnidadeDeMedida;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Data Transfer Object (DTO) para a resposta da simulação de uma produção por consumo.
 * <p>
 * Este DTO informa a quantidade total de matéria-prima que seria necessária para produzir
 * uma determinada quantidade de um produto, sem efetivamente movimentar o estoque.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulacaoConsumoResponseDTO {

    @Schema(description = "Campo discriminador para identificar o tipo de resultado da simulação no frontend.", example = "CONSUMO", accessMode = Schema.AccessMode.READ_ONLY)
    private final String tipoSimulacao = "CONSUMO";

    @Schema(description = "Consumo total estimado de matéria-prima, na unidade de consumo padrão do material.", example = "10.0")
    private BigDecimal consumoTotalEstimado;

    @Schema(description = "Unidade de medida do consumo estimado.", example = "LITRO")
    private UnidadeDeMedida unidadeDeConsumo;

    @Schema(description = "Descrição amigável da unidade de medida do consumo estimado.", example = "Litro")
    private String unidadeDescricao;

    @Schema(description = "Plano detalhado de quanto será consumido de cada lote.")
    private List<PlanoDeConsumoItemDTO> planoDeConsumo;

    @Schema(description = "Mapa com o saldo restante projetado para cada lote após a simulação.")
    private Map<Long, BigDecimal> saldoRestante;
}
