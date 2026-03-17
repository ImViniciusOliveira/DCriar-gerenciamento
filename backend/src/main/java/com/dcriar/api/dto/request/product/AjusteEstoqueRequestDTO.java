package com.dcriar.api.dto.request.product;

import com.dcriar.api.validation.annotation.ValidAjusteEstoque;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * Data Transfer Object (DTO) para ajustar o estoque de um produto em um canal de venda específico.
 * <p>
 * Este DTO é usado para alocar ou desalocar unidades de um produto para um canal de venda,
 * movendo o estoque entre o estoque mestre e o estoque do canal. A validação dos campos
 * é garantida pela anotação {@link ValidAjusteEstoque}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ValidAjusteEstoque
public class AjusteEstoqueRequestDTO {

    /**
     * O ID do produto cujo estoque será ajustado.
     */
    @Schema(description = "ID do produto cujo estoque no canal será ajustado.", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long produtoId;

    /**
     * O ID do canal de venda onde o estoque será ajustado.
     */
    @Schema(description = "ID do canal de venda onde o estoque será ajustado.", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long canalVendaId;

    /**
     * A quantidade a ser ajustada.
     * <p>
     * Use um valor positivo para adicionar estoque ao canal (alocar) e um valor
     * negativo para remover estoque do canal (desalocar).
     */
    @Schema(description = "Quantidade a ser ajustada. Use valor positivo para alocar e negativo para desalocar estoque.", example = "-1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer quantidade;
}
