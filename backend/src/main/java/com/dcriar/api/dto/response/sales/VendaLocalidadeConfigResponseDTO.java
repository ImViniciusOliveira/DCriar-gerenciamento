package com.dcriar.api.dto.response.sales;

import com.dcriar.domain.sales.entity.enums.ModoLocalidadeVenda;
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
public class VendaLocalidadeConfigResponseDTO {

    @Schema(description = "País resolvido pelo backend para a venda.", example = "Brasil")
    private String pais;

    @Schema(description = "Modo de localidade que o frontend deve usar.", example = "BRASIL")
    private ModoLocalidadeVenda modoLocalidade;

    @Schema(description = "Lista de estados brasileiros disponível quando o país usar o modo Brasil.")
    private List<VendaEstadoOptionResponseDTO> estadosBrasil;
}
