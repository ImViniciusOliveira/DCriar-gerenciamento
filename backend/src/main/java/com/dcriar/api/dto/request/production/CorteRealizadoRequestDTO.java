package com.dcriar.api.dto.request.production;

import com.dcriar.api.validation.annotation.ValidCorteRealizadoRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.math.BigDecimal;

/**
 * DTO para entrada de dados de CorteRealizado.
 * <p>
 * Utilizado para criação e atualização de cortes realizados em uma ordem de produção.
 * Todos os campos possuem validação e documentação para uso seguro na API.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ValidCorteRealizadoRequest
public class CorteRealizadoRequestDTO {
    /**
     * ID da ordem de produção à qual o corte está associado.
     */
    @Schema(description = "ID da ordem de produção à qual o corte está associado.", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long ordemDeProducaoId;

    /**
     * Largura do corte em centímetros.
     */
    @Schema(description = "Largura do corte em centímetros.", example = "10.5", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal larguraCm;

    /**
     * Comprimento do corte em centímetros.
     */
    @Schema(description = "Comprimento do corte em centímetros.", example = "15.0", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal comprimentoCm;

    /**
     * Quantidade de peças produzidas com este corte.
     */
    @Schema(description = "Quantidade de peças produzidas com este corte.", example = "50", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer quantidade;

    /**
     * Tipo de resultado do corte (\"PRODUTO\" ou \"RETALHO\").
     */
    @Schema(description = "Tipo de resultado do corte (\"PRODUTO\" ou \"RETALHO\").", example = "PRODUTO", requiredMode = Schema.RequiredMode.REQUIRED)
    private String tipo;

    /**
     * Categoria do retalho quando {@code tipo == "RETALHO"}.
     * Valores sugeridos: "LATERAL", "FINAL". Nulo para produtos.
     */
    @Schema(description = "Categoria do retalho quando o tipo é \"RETALHO\". Valores sugeridos: \"LATERAL\", \"FINAL\". Nulo para produtos.", example = "LATERAL")
    private String retalhoCategoria;

    /**
     * Quantidade de repetições deste corte (para agrupamento de linhas iguais).
     */
    @Schema(description = "Quantidade de repetições deste corte (para agrupamento de linhas iguais).", example = "41")
    private Integer repeticoes;
}
