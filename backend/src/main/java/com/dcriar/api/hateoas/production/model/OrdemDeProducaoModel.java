package com.dcriar.api.hateoas.production.model;

import com.dcriar.api.dto.response.production.CorteRealizadoResponseDTO;
import com.dcriar.api.dto.response.production.DetalhesCorteResponseDTO;
import com.dcriar.domain.production.enums.ModoCalculo;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Modelo de representação HATEOAS para uma Ordem de Produção.
 */
@Getter
@Setter
@JsonRootName(value = "ordemDeProducao")
@Relation(collectionRelation = "ordensDeProducao", itemRelation = "ordemDeProducao")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrdemDeProducaoModel extends RepresentationModel<OrdemDeProducaoModel> {

    @Schema(description = "ID único da ordem de produção.")
    private Long id;

    @Schema(description = "ID do produto final fabricado.")
    private Long produtoId;

    @Schema(description = "Nome do produto final fabricado.")
    private String nomeProduto;

    @Schema(description = "Tipo do produto fabricado.", example = "CORTE")
    private String tipoProduto;

    @Schema(description = "Lista de IDs dos lotes de matéria-prima consumidos.")
    private List<Long> lotesConsumidosIds;

    @Schema(description = "ID do canal de venda para o qual o estoque produzido foi destinado.", nullable = true)
    private Long canalVendaDestinoId;

    @Schema(description = "Quantidade de unidades do produto que foram produzidas.")
    private Integer quantidadeProduzida;

    @Schema(description = "Modo de cálculo utilizado (relevante para ordens de corte).")
    private ModoCalculo modoCalculo;

    @Schema(description = "Data e hora em que a ordem foi criada.")
    private LocalDateTime dataCriacao;

    @Schema(description = "Data e hora da última atualização da ordem.")
    private LocalDateTime dataAtualizacao;

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
