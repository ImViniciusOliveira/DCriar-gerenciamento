package com.dcriar.api.dto.request.product;

import com.dcriar.api.validation.annotation.ValidCanalVendaRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * DTO para requisições de criação/atualização de CanalVenda.
 * <p>
 * Centraliza validações e requisitos de negócio para entrada de dados.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ValidCanalVendaRequest
public class CanalVendaRequestDTO {
    /**
     * O nome único do canal de venda (ex: "SHOPEE", "MERCADO_LIVRE", "LOJA_FISICA").
     */
    @Schema(description = "O nome único do canal de venda.", example = "SHOPEE", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nome;
}
