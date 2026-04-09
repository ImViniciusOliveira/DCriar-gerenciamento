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
public class AjusteEstoqueProdutoResumoDTO {

    @Schema(description = "ID do produto.", example = "11")
    private Long produtoId;

    @Schema(description = "Nome do produto.", example = "Kit de Resina Epóxi")
    private String nomeProduto;

    @Schema(description = "SKU do produto.", example = "RES-EPX-2KG")
    private String skuProduto;

    @Schema(description = "Estoque físico total do produto.", example = "8")
    private Integer estoqueFisicoTotal;

    @Schema(description = "Estoque total já distribuído nos canais.", example = "5")
    private Integer estoqueDistribuidoTotal;

    @Schema(description = "Saldo ainda disponível para distribuição.", example = "3")
    private Integer estoqueDisponivelParaAlocar;

    @Schema(description = "Status de consistência do estoque para a tela de ajustes.", example = "CONSISTENTE")
    private String statusDivergencia;

    @Schema(description = "Ações atualmente bloqueadas para esse produto na UI.", example = "[\"ajusteFisicoNegativo\", \"usarEmProducao\"]")
    private Set<String> camposBloqueados;

    @Schema(description = "Motivos por ação bloqueada, para orientar a UI.", example = "{\"ajusteFisicoNegativo\":\"Nao e permitido aprofundar saldo fisico negativo.\"}")
    private Map<String, String> motivosBloqueio;
}
