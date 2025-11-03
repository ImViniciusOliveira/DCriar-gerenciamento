package com.dcriar.api.dto.response.production;

import com.dcriar.domain.production.enums.ModoCalculo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.hateoas.RepresentationModel;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * DTO para a resposta de uma Ordem de Produção.
 * <p>
 * Fornece uma visão detalhada de uma ordem de produção, seja ela de corte
 * ou de consumo direto, incluindo os lotes consumidos e os produtos gerados.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Builder
public class OrdemDeProducaoResponseDTO extends RepresentationModel<OrdemDeProducaoResponseDTO> {

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

    @Schema(description = "Modo de cálculo utilizado (relevante para ordens de corte).")
    private ModoCalculo modoCalculo;

    @Schema(description = "Data e hora em que a ordem foi criada.")
    private OffsetDateTime dataCriacao;

    @Schema(description = "Motivo ou referência para a ordem.")
    private String motivo;

    @Schema(description = "Largura final do corte em cm (se aplicável).", nullable = true)
    private BigDecimal larguraFinalCm;

    @Schema(description = "Comprimento final do corte em cm (se aplicável).", nullable = true)
    private BigDecimal comprimentoFinalCm;

    @Schema(description = "Indica se a peça foi rotacionada para melhor aproveitamento.", nullable = true)
    private Boolean rotacionado;

    @Schema(description = "Lista detalhada de todos os cortes realizados (produtos e retalhos).", nullable = true)
    private List<CorteRealizadoResponseDTO> cortesRealizados;

    @Schema(description = "Detalhes sobre a otimização do corte, se aplicável.", nullable = true)
    private DetalhesCorteResponseDTO detalhesCorte;
}
