package com.dcriar.api.dto.request.sales;

import com.dcriar.api.validation.annotation.ValidVendaRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.*;

import java.util.List;

/**
 * Data Transfer Object (DTO) para registrar uma nova Venda.
 * <p>
 * Este DTO é utilizado para receber os dados de uma nova venda, incluindo o canal
 * onde a transação ocorreu e a lista de itens vendidos. A validação dos dados é
 * garantida pela anotação customizada {@link ValidVendaRequest}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ValidVendaRequest
public class VendaRequestDTO {

    /**
     * O ID do canal de venda onde a transação ocorreu.
     */
    @Schema(description = "O ID do canal de venda onde a transação ocorreu. No seed padrão, use 1 para 'Loja Física'.", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long canalVendaId;

    /**
     * A lista de itens que compõem a venda.
     * <p>
     * A anotação {@code @Valid} garante que cada item da lista seja validado individualmente.
     */
    @Schema(description = "A lista de itens que compõem a venda. No seed padrão, um caso feliz é vender 1 unidade do produto 11 na Loja Física.", example = "[{\"produtoId\":11,\"quantidade\":1}]", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<@Valid ItemVendaRequestDTO> itens;
}
