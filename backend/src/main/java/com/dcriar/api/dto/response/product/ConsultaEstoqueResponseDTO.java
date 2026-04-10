package com.dcriar.api.dto.response.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultaEstoqueResponseDTO {

    @Schema(description = "ID do produto.", example = "11")
    private Long produtoId;

    @Schema(description = "Nome do produto.", example = "Kit de Resina Epóxi")
    private String nomeProduto;

    @Schema(description = "SKU do produto.", example = "RES-EPX-2KG")
    private String skuProduto;

    @Schema(description = "ID do canal de venda.", example = "1")
    private Long canalVendaId;

    @Schema(description = "Nome do canal de venda.", example = "Loja Física")
    private String nomeCanalVenda;

    @Schema(description = "Quantidade atualmente alocada nesse canal.", example = "2")
    private Integer quantidadeNoCanal;

    @Schema(description = "Estoque físico total do produto.", example = "8")
    private Integer estoqueFisicoTotal;

    @Schema(description = "Estoque total já distribuído nos canais.", example = "5")
    private Integer estoqueDistribuidoTotal;

    @Schema(description = "Saldo ainda disponível para distribuição.", example = "3")
    private Integer estoqueDisponivelParaAlocar;

    @Schema(description = "Status de consistência do estoque para a tela de consultas.", example = "CONSISTENTE")
    private String statusDivergencia;

    @Schema(description = "Ações atualmente bloqueadas para esse contexto de canal na UI.", example = "[\"ajusteCanalPositivo\"]")
    private Set<String> camposBloqueados;

    @Schema(description = "Motivos por ação bloqueada, para orientar a UI.", example = "{\"ajusteCanalPositivo\":\"Produto com distribuicao acima do estoque fisico.\"}")
    private Map<String, String> motivosBloqueio;
}
