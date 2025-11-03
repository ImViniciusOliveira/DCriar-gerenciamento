package com.dcriar.api.hateoas.product.model;

import com.dcriar.api.dto.response.product.MovimentacaoProdutoResponseDTO;
import com.dcriar.domain.product.entity.enums.TipoMovimentacaoProduto;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

import java.time.OffsetDateTime;

/**
 * Representa o modelo de recurso HATEOAS para uma movimentação de estoque de produto.
 * Este modelo estende {@link RepresentationModel} para incluir links HATEOAS
 * e é usado para representar informações detalhadas sobre uma movimentação específica
 * no histórico do estoque mestre de um produto.
 */
@Getter
@Setter
@Builder
@Relation(collectionRelation = "movimentacoes")
public class MovimentacaoProdutoModel extends RepresentationModel<MovimentacaoProdutoModel> {

    /**
     * O ID único da movimentação de estoque do produto.
     */
    private Long id;

    /**
     * A data e hora em que a movimentação foi registrada.
     */
    private OffsetDateTime data;

    /**
     * O tipo da movimentação do estoque (ex: ENTRADA_PRODUCAO, SAIDA_VENDA).
     */
    private TipoMovimentacaoProduto tipo;

    /**
     * A quantidade de unidades do produto que foi movimentada.
     * O valor é positivo para entradas (ex: produção) e negativo para saídas (ex: venda).
     */
    private Integer quantidade;

    /**
     * O motivo ou observação associado à movimentação, como o ID da ordem de corte ou da venda.
     */
    private String motivo;

    /**
     * Construtor privado para uso do Lombok Builder.
     *
     * @param id O ID único da movimentação.
     * @param data A data e hora da movimentação.
     * @param tipo O tipo da movimentação.
     * @param quantidade A quantidade movimentada.
     * @param motivo O motivo da movimentação.
     */
    private MovimentacaoProdutoModel(Long id, OffsetDateTime data, TipoMovimentacaoProduto tipo, Integer quantidade, String motivo) {
        this.id = id;
        this.data = data;
        this.tipo = tipo;
        this.quantidade = quantidade;
        this.motivo = motivo;
    }

    /**
     * Cria uma instância de {@link MovimentacaoProdutoModel} a partir de um {@link MovimentacaoProdutoResponseDTO}.
     * Utiliza o padrão Builder para construir o objeto de forma mais legível e flexível.
     *
     * @param dto O DTO de resposta da movimentação contendo os dados.
     * @return Uma nova instância de {@link MovimentacaoProdutoModel} preenchida com os dados do DTO.
     */
    public static MovimentacaoProdutoModel fromDto(MovimentacaoProdutoResponseDTO dto) {
        return MovimentacaoProdutoModel.builder()
                .id(dto.getId())
                .data(dto.getData())
                .tipo(dto.getTipo())
                .quantidade(dto.getQuantidade())
                .motivo(dto.getMotivo())
                .build();
    }
}
