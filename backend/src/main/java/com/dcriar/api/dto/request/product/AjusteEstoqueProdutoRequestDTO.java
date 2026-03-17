package com.dcriar.api.dto.request.product;

import com.dcriar.api.validation.annotation.ValidAjusteEstoqueProduto;
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

    /**
     * A quantidade a ser ajustada.
     * <p>
     * Use um valor positivo para adicionar (entrada) e um valor negativo para remover (saída).
     */
    @Schema(description = "Quantidade a ser ajustada. Use valor positivo para entrada e negativo para saída.", example = "-1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer quantidade;

    /**
     * O motivo do ajuste manual, para fins de auditoria e rastreabilidade.
     */
    @Schema(description = "O motivo do ajuste manual, para fins de auditoria e rastreabilidade.", example = "Correção de inventário após contagem física.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String motivo;

}
