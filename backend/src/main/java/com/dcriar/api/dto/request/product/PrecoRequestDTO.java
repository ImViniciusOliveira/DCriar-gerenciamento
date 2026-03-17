package com.dcriar.api.dto.request.product;

import com.dcriar.api.validation.annotation.ValidPrecoRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.math.BigDecimal;

/**
 * DTO para entrada de dados de preço de produto.
 * <p>
 * Utilizado para criação e atualização de preços, centralizando validações e estrutura de dados.
 * Todos os campos devem ser validados conforme regras de negócio e documentados para uso na API.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ValidPrecoRequest
public class PrecoRequestDTO {
    /**
     * ID do produto ao qual o preço está associado.
     * <p>Obrigatório para vincular o preço ao produto correto.</p>
     */
    @Schema(description = "ID do produto ao qual o preço está associado.", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long produtoId;

    /**
     * Tipo de preço (ex: VAREJO, ATACADO).
     * <p>Deve corresponder ao enum TipoPreco.</p>
     */
    @Schema(description = "Tipo de preço (ex: VAREJO, ATACADO).", example = "VAREJO", requiredMode = Schema.RequiredMode.REQUIRED)
    private String tipoPreco;

    /**
     * Valor base do preço do produto.
     * <p>Deve ser maior ou igual a zero.</p>
     */
    @Schema(description = "Valor base do preço do produto.", example = "120.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal valor;

    /**
     * Valor promocional do produto, se houver promoção ativa.
     * <p>Deve ser maior ou igual a zero.</p>
     */
    @Schema(description = "Valor promocional do produto, se houver promoção ativa.", example = "99.90")
    private BigDecimal valorPromocional;

    /**
     * Indica se a promoção está ativa.
     * <p>Obrigatório para definir se o valor promocional será aplicado.</p>
     */
    @Schema(description = "Indica se a promoção está ativa.", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean promocaoAtiva;
}
