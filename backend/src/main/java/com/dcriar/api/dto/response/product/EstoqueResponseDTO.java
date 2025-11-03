package com.dcriar.api.dto.response.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * Data Transfer Object (DTO) que representa a resposta do estoque de um produto em um canal de venda.
 * <p>
 * Este DTO fornece uma visão consolidada do saldo de um produto específico em um
 * determinado canal de venda, facilitando a consulta de disponibilidade.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstoqueResponseDTO {

    /**
     * O ID único do registro de estoque.
     */
    @Schema(description = "ID único do registro de estoque.", example = "1")
    private Long id;

    /**
     * O ID do produto associado a este registro de estoque.
     */
    @Schema(description = "ID do produto associado a este estoque.", example = "1")
    private Long produtoId;

    /**
     * O nome do produto para fácil identificação.
     */
    @Schema(description = "Nome do produto.", example = "Etiqueta Redonda Kraft 5x5cm")
    private String nomeProduto;

    /**
     * O ID do canal de venda onde este estoque está alocado.
     */
    @Schema(description = "ID do canal de venda.", example = "2")
    private Long canalVendaId;

    /**
     * O nome do canal de venda para fácil identificação.
     */
    @Schema(description = "Nome do canal de venda.", example = "SHOPEE")
    private String nomeCanalVenda;

    /**
     * A quantidade de unidades do produto disponíveis neste canal de venda.
     */
    @Schema(description = "Quantidade disponível deste produto neste canal.", example = "50")
    private Integer quantidade;
}
