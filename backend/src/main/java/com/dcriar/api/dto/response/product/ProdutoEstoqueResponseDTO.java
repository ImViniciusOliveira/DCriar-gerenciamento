package com.dcriar.api.dto.response.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * DTO (Data Transfer Object) que agrupa o estoque de um único produto por todos os seus canais de venda.
 * Este formato é otimizado para o frontend, que pode solicitar o estoque de um produto
 * e receber todas as suas alocações de uma só vez.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProdutoEstoqueResponseDTO {

    /**
     * O ID do produto ao qual este agrupamento de estoque se refere.
     */
    @Schema(description = "O ID do produto.", example = "1")
    private Long produtoId;

    /**
     * Uma lista contendo o detalhamento do estoque para cada canal de venda.
     */
    @Schema(description = "Lista de estoques por canal de venda.")
    private List<CanalEstoqueResponseDTO> canais;
}
