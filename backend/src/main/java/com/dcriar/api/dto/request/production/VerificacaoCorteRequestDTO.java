package com.dcriar.api.dto.request.production;

import com.dcriar.api.validation.annotation.ValidVerificacaoCorteRequest;
import com.dcriar.domain.production.enums.ModoCalculo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Data Transfer Object (DTO) para solicitar a verificação de um layout de corte editado.
 * <p>
 * Este DTO é utilizado quando o usuário altera manualmente os parâmetros de uma simulação
 * (como margens ou dimensões manuais) e deseja validar se o novo layout é viável e quais
 * são os seus impactos no consumo e sobras.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ValidVerificacaoCorteRequest
public class VerificacaoCorteRequestDTO {

    @Schema(description = "ID do produto.", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long produtoId;

    @Schema(description = "ID do lote de matéria-prima.", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long loteId;

    @Schema(description = "Quantidade de unidades desejada.", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer quantidade;

    @Schema(description = "Modo de cálculo (AUTOMATICO ou MANUAL).", example = "AUTOMATICO", requiredMode = Schema.RequiredMode.REQUIRED)
    private ModoCalculo modoCalculo;

    @Schema(description = "Margens de segurança (relevante no modo AUTOMATICO).")
    private MargensRequestDTO margens;

    @Schema(description = "Largura do bloco de produtos no corte (em cm).", example = "20.0")
    private BigDecimal larguraBlocoProdutosCm;

    @Schema(description = "Comprimento do bloco de produtos no corte (em cm).", example = "15.0")
    private BigDecimal comprimentoBlocoProdutosCm;

    @Schema(description = "ID opcional da ordem de produção em edição, para considerar o estorno no cálculo.", example = "5")
    private Long ordemId;
}
