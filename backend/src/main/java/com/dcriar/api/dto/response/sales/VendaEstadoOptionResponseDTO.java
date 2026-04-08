package com.dcriar.api.dto.response.sales;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendaEstadoOptionResponseDTO {

    @Schema(description = "Sigla UF do estado.", example = "SP")
    private String uf;

    @Schema(description = "Nome de exibição do estado.", example = "São Paulo")
    private String nome;
}
