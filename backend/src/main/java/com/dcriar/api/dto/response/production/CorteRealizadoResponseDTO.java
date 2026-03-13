package com.dcriar.api.dto.response.production;

import lombok.*;
import java.math.BigDecimal;

/**
 * DTO para saída de dados de CorteRealizado.
 * <p>
 * Utilizado para retornar informações detalhadas sobre cortes realizados em uma ordem de produção.
 * Todos os campos refletem o estado atual do corte realizado e são documentados para uso na API.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CorteRealizadoResponseDTO {
    /**
     * ID do corte realizado.
     */
    private Long id;

    /**
     * ID da ordem de produção à qual o corte está associado.
     */
    private Long ordemDeProducaoId;

    /**
     * Largura do corte em centímetros.
     */
    private BigDecimal larguraCm;

    /**
     * Comprimento do corte em centímetros.
     */
    private BigDecimal comprimentoCm;

    /**
     * Quantidade de peças produzidas com este corte.
     */
    private Integer quantidade;

    /**
     * Tipo de resultado do corte ("PRODUTO" ou "RETALHO").
     */
    private String tipo;

    /**
     * Categoria do retalho quando {@code tipo == "RETALHO"}.
     * Valores sugeridos: "LATERAL", "FINAL". Nulo para produtos.
     */
    private String retalhoCategoria;

    /**
     * Quantidade de repetições deste corte (para agrupamento de linhas iguais).
     */
    private Integer repeticoes;
}
