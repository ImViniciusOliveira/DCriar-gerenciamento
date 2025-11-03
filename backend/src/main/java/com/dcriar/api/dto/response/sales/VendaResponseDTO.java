package com.dcriar.api.dto.response.sales;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Data Transfer Object (DTO) que representa a resposta de uma Venda finalizada.
 * <p>
 * Este DTO fornece uma representação completa da transação, incluindo o canal de venda,
 * o valor total e a lista detalhada de todos os itens vendidos.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendaResponseDTO {

    /**
     * O ID único da venda.
     */
    @Schema(description = "O ID único da venda.", example = "1")
    private Long id;

    /**
     * A data e hora em que a venda foi registrada no sistema.
     */
    @Schema(description = "A data e hora em que a venda foi registrada no sistema.")
    private OffsetDateTime dataVenda;

    /**
     * O nome do canal de venda onde a transação ocorreu.
     */
    @Schema(description = "O nome do canal de venda onde a transação ocorreu.", example = "SHOPEE")
    private String nomeCanalVenda;

    /**
     * O valor total da venda, somando todos os itens.
     */
    @Schema(description = "O valor total da venda.", example = "99.90")
    private BigDecimal valorTotal;

    /**
     * A lista de itens que compõem a venda.
     */
    @Schema(description = "A lista de itens que foram vendidos nesta transação.")
    private List<ItemVendaResponseDTO> itens;
}
