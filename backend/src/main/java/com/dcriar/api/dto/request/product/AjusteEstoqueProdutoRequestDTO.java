package com.dcriar.api.dto.request.product;

import com.dcriar.api.validation.annotation.ValidAjusteEstoqueProduto;
import com.dcriar.domain.product.entity.enums.DirecaoAjusteEstoque;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * Data Transfer Object (DTO) para uma requisição de ajuste manual no estoque físico (mestre) de um produto.
 * <p>
 * Este DTO é usado para operações de entrada ou saída diretas no estoque mestre,
 * como correções de inventário, registro de perdas ou entradas de produção não rastreadas
 * por uma ordem de corte. Toda operação requer um motivo para fins de auditoria.
 * A validação dos campos é garantida pela anotação {@link ValidAjusteEstoqueProduto}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ValidAjusteEstoqueProduto
public class AjusteEstoqueProdutoRequestDTO {

    /**
     * O ID do produto cujo estoque físico (mestre) será ajustado.
     */
    @Schema(description = "ID do produto cujo estoque físico será ajustado.", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long produtoId;

    @Schema(description = "Direção do ajuste. Use ADICIONAR para entrada e RETIRAR para saída.", example = "ADICIONAR", requiredMode = Schema.RequiredMode.REQUIRED)
    private DirecaoAjusteEstoque direcao;

    /**
     * A quantidade a ser ajustada.
     * <p>
     * Sempre informe um valor positivo. A direção define se a operação é de entrada ou saída.
     */
    @Schema(description = "Quantidade absoluta a ser ajustada. Sempre positiva.", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer quantidade;

    /**
     * O motivo do ajuste manual, para fins de auditoria e rastreabilidade.
     */
    @Schema(description = "O motivo do ajuste manual, para fins de auditoria e rastreabilidade.", example = "Correção de inventário após contagem física.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String motivo;

}
