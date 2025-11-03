package com.dcriar.api.dto.response.product;

import lombok.*;

/**
 * DTO de resposta para CanalVenda.
 * <p>
 * Utilizado para retornar dados do canal de venda na API.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CanalVendaResponseDTO {
    /**
     * O ID único do canal de venda.
     */
    private Long id;

    /**
     * O nome único do canal de venda.
     */
    private String nome;
}
