package com.dcriar.api.hateoas.product.model;

import com.dcriar.api.dto.response.product.EstoqueResponseDTO;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

/**
 * Representa o modelo de recurso HATEOAS para um estoque de produto.
 * Este modelo estende {@link RepresentationModel} para incluir links HATEOAS
 * e é usado para representar informações detalhadas sobre o estoque de um produto
 * em um canal de venda específico.
 */
@Getter
@Setter
@Builder
@Relation(collectionRelation = "estoques")
public class EstoqueProdutoModel extends RepresentationModel<EstoqueProdutoModel> {

    /**
     * O identificador único do registro de estoque.
     */
    private Long id;

    /**
     * O identificador do produto associado a este estoque.
     */
    private Long produtoId;

    /**
     * O nome do produto.
     */
    private String nomeProduto;

    /**
     * O identificador do canal de venda onde o produto está estocado.
     */
    private Long canalVendaId;

    /**
     * O nome do canal de venda.
     */
    private String nomeCanalVenda;

    /**
     * A quantidade atual do produto em estoque neste canal.
     */
    private Integer quantidade;

    /**
     * Construtor privado para uso do Lombok Builder.
     *
     * @param id O identificador único do registro de estoque.
     * @param produtoId O identificador do produto associado a este estoque.
     * @param nomeProduto O nome do produto.
     * @param canalVendaId O identificador do canal de venda onde o produto está estocado.
     * @param nomeCanalVenda O nome do canal de venda.
     * @param quantidade A quantidade atual do produto em estoque neste canal.
     */
    private EstoqueProdutoModel(Long id, Long produtoId, String nomeProduto, Long canalVendaId, String nomeCanalVenda, Integer quantidade) {
        this.id = id;
        this.produtoId = produtoId;
        this.nomeProduto = nomeProduto;
        this.canalVendaId = canalVendaId;
        this.nomeCanalVenda = nomeCanalVenda;
        this.quantidade = quantidade;
    }

    /**
     * Cria uma instância de {@link EstoqueProdutoModel} a partir de um {@link EstoqueResponseDTO}.
     * Utiliza o padrão Builder para construir o objeto de forma mais legível e flexível.
     *
     * @param dto O DTO de resposta de estoque contendo os dados.
     * @return Uma nova instância de {@link EstoqueProdutoModel} preenchida com os dados do DTO.
     */
    public static EstoqueProdutoModel fromDto(EstoqueResponseDTO dto) {
        return EstoqueProdutoModel.builder()
                .id(dto.getId())
                .produtoId(dto.getProdutoId())
                .nomeProduto(dto.getNomeProduto())
                .canalVendaId(dto.getCanalVendaId())
                .nomeCanalVenda(dto.getNomeCanalVenda())
                .quantidade(dto.getQuantidade())
                .build();
    }
}
