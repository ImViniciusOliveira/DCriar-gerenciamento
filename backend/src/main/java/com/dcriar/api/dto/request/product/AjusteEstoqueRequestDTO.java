package com.dcriar.api.dto.request.product;

import com.dcriar.api.validation.annotation.ValidAjusteEstoque;
import com.dcriar.domain.product.entity.enums.DirecaoAjusteEstoque;
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

    @Schema(description = "Direção do ajuste. Use ADICIONAR para alocar no canal e RETIRAR para desalocar do canal.", example = "ADICIONAR", requiredMode = Schema.RequiredMode.REQUIRED)
    private DirecaoAjusteEstoque direcao;

    /**
     * A quantidade a ser ajustada.
     * <p>
     * Sempre informe um valor positivo. A direção define se a operação aloca ou desaloca.
     */
    @Schema(description = "Quantidade absoluta a ser ajustada. Sempre positiva.", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer quantidade;
}
