package com.dcriar.api.dto.response.production;

import com.dcriar.domain.production.enums.ModoCalculo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Data Transfer Object (DTO) para a resposta da simulação de uma produção por corte.
 * <p>
 * Este DTO informa as dimensões otimizadas do corte e o consumo de matéria-prima estimado
 * para produzir uma determinada quantidade de um produto, sem efetivamente criar uma ordem de produção.
 */
@Getter
@Setter
@Builder
public class SimulacaoCorteResponseDTO {

    @Schema(description = "Modo de cálculo sugerido ou utilizado na simulação.", example = "AUTOMATICO")
    private ModoCalculo modoCalculo;

    @Schema(description = "Largura final calculada para o corte (em cm).", example = "80.0")
    private BigDecimal larguraFinalCm;

    @Schema(description = "Comprimento final calculado para o corte (em cm).", example = "1200.0")
    private BigDecimal comprimentoFinalCm;

    @Schema(description = "Consumo estimado de matéria-prima (na unidade de estoque do lote, ex: metros lineares).", example = "12.0000")
    private BigDecimal consumoEstimado;

    @Schema(description = "Indica se o layout otimizado dos produtos foi rotacionado para melhor aproveitamento.", example = "true")
    private boolean rotacionado;

    @Schema(description = "Quantidade máxima de produtos que cabem em uma única linha.", example = "2")
    private Integer produtosPorLinha;

    @Schema(description = "Quantidade de linhas que estão totalmente preenchidas com produtos.", example = "5")
    private Integer numeroLinhasCompletas;

    @Schema(description = "Quantidade de produtos presentes na última linha (pode ser parcial).", example = "1")
    private Integer produtosNaUltimaLinha;

    @Schema(description = "Descrição formatada da tira de sobra contínua (r1).", example = "10cm x 30cm")
    private String sobraLateral;

    @Schema(description = "Descrição formatada do bloco de sobra na última linha (r2).", example = "54cm x 10cm")
    private String sobraInferior;

    @Schema(description = "Descrição formatada da sobra de comprimento do rolo.", example = "100cm x 49.7m")
    private String saldoRolo;

    @Schema(description = "Descrição formatada das dimensões unitárias do produto.", example = "8cm x 12cm")
    private String dimensaoProduto;

    @Schema(description = "Descrição formatada das dimensões totais de matéria-prima consumida.", example = "160cm x 804cm")
    private String consumoTotal;

    @Schema(description = "Campo discriminador para identificar o tipo de resultado da simulação no frontend.", example = "CORTE", accessMode = Schema.AccessMode.READ_ONLY)
    private final String tipoSimulacao = "CORTE";

    @Schema(description = "Largura do bloco de produtos no corte (em cm).", example = "75.0")
    private BigDecimal larguraBlocoProdutosCm;

    @Schema(description = "Comprimento do bloco de produtos no corte (em cm).", example = "1150.0")
    private BigDecimal comprimentoBlocoProdutosCm;
}
