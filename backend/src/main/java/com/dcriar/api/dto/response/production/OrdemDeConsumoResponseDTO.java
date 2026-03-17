package com.dcriar.api.dto.response.production;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.hateoas.RepresentationModel;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO específico para a resposta de criação de uma Ordem de Produção por Consumo.
 * Retorna apenas os campos que fazem sentido para este fluxo.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrdemDeConsumoResponseDTO extends RepresentationModel<OrdemDeConsumoResponseDTO> {

    @Schema(description = "ID único da ordem de produção.")
    private Long id;

    @Schema(description = "ID do produto final fabricado.")
    private Long produtoId;

    @Schema(description = "Nome do produto final fabricado.")
    private String nomeProduto;

    @Schema(description = "Lista de IDs dos lotes de matéria-prima consumidos.")
    private List<Long> lotesConsumidosIds;

    @Schema(description = "Quantidade de unidades do produto que foram produzidas.")
    private Integer quantidadeProduzida;

    @Schema(description = "Data e hora em que a ordem foi criada.")
    private LocalDateTime dataCriacao;

    @Schema(description = "Data e hora da última atualização da ordem.")
    private LocalDateTime dataAtualizacao;

    @Schema(description = "Motivo ou referência para a ordem.")
    private String motivo;
}
