package com.dcriar.api.hateoas.product.model;

import com.dcriar.api.dto.response.product.ConsultaEstoqueResponseDTO;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

import java.util.Map;
import java.util.Set;

@Getter
@Setter
@Builder
@Relation(collectionRelation = "consultasEstoque")
public class ConsultaEstoqueModel extends RepresentationModel<ConsultaEstoqueModel> {

    private Long produtoId;
    private String nomeProduto;
    private String skuProduto;
    private Long canalVendaId;
    private String nomeCanalVenda;
    private Integer quantidadeNoCanal;
    private Integer estoqueFisicoTotal;
    private Integer estoqueDistribuidoTotal;
    private Integer estoqueDisponivelParaAlocar;
    private String statusDivergencia;
    private Set<String> camposBloqueados;
    private Map<String, String> motivosBloqueio;

    public static ConsultaEstoqueModel fromDto(ConsultaEstoqueResponseDTO dto) {
        return ConsultaEstoqueModel.builder()
                .produtoId(dto.getProdutoId())
                .nomeProduto(dto.getNomeProduto())
                .skuProduto(dto.getSkuProduto())
                .canalVendaId(dto.getCanalVendaId())
                .nomeCanalVenda(dto.getNomeCanalVenda())
                .quantidadeNoCanal(dto.getQuantidadeNoCanal())
                .estoqueFisicoTotal(dto.getEstoqueFisicoTotal())
                .estoqueDistribuidoTotal(dto.getEstoqueDistribuidoTotal())
                .estoqueDisponivelParaAlocar(dto.getEstoqueDisponivelParaAlocar())
                .statusDivergencia(dto.getStatusDivergencia())
                .camposBloqueados(dto.getCamposBloqueados())
                .motivosBloqueio(dto.getMotivosBloqueio())
                .build();
    }
}
