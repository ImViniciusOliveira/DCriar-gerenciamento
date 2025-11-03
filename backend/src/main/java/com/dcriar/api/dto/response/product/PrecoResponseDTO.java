package com.dcriar.api.dto.response.product;

import lombok.*;
import java.math.BigDecimal;

/**
 * DTO para saída de dados de preço de produto.
 * <p>
 * Utilizado para retornar informações detalhadas sobre o preço de um produto,
 * incluindo valores promocionais e status de promoção.
 * Todos os campos refletem o estado atual do preço cadastrado no sistema e são documentados para uso na API.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrecoResponseDTO {
    /**
     * ID único do preço.
     * <p>Identificador gerado automaticamente para cada registro de preço.</p>
     */
    private Long id;

    /**
     * ID do produto ao qual o preço está associado.
     * <p>Permite relacionar o preço ao produto correspondente.</p>
     */
    private Long produtoId;

    /**
     * Tipo de preço (ex: VAREJO, ATACADO).
     * <p>Indica a modalidade de precificação aplicada ao produto.</p>
     */
    private String tipoPreco;

    /**
     * Valor base do preço do produto.
     * <p>Representa o valor principal cobrado pelo produto, sem descontos.</p>
     */
    private BigDecimal valor;

    /**
     * Valor promocional do produto, se houver promoção ativa.
     * <p>Caso a promoção esteja ativa, este campo indica o valor com desconto.</p>
     */
    private BigDecimal valorPromocional;

    /**
     * Indica se a promoção está ativa para este preço.
     * <p>Se verdadeiro, o valor promocional é aplicado nas vendas.</p>
     */
    private Boolean promocaoAtiva;
}
