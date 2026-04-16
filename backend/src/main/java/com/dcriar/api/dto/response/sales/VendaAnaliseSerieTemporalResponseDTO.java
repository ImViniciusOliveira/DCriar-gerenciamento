package com.dcriar.api.dto.response.sales;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendaAnaliseSerieTemporalResponseDTO {

    @Schema(description = "Descrição do agrupamento aplicado à série (ex: 'Receita diária', 'Receita semanal', 'Receita mensal').", example = "Receita diária")
    private String trendLabel;

    @Schema(description = "Pontos da série temporal, ordenados cronologicamente.")
    private List<VendaAnaliseSerieItemResponseDTO> serie;
}
