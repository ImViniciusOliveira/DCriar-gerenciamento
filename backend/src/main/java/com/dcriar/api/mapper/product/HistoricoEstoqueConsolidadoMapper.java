package com.dcriar.api.mapper.product;

import com.dcriar.api.dto.response.product.HistoricoEstoqueConsolidadoResponseDTO;
import com.dcriar.domain.product.entity.MovimentacaoEstoqueProduto;
import com.dcriar.domain.product.entity.enums.TipoMovimentacaoProduto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

/**
 * Mapper responsável por transformar a entidade de movimentação de estoque
 * em um DTO consolidado para a listagem histórica paginada.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface HistoricoEstoqueConsolidadoMapper {

    @Mapping(source = "produto.id", target = "produtoId")
    @Mapping(source = "produto.nome", target = "produtoNome")
    @Mapping(source = "produto.sku", target = "produtoSku")
    @Mapping(source = "ordemProducaoOrigemId", target = "ordemProducaoId")
    @Mapping(source = "vendaOrigemId", target = "vendaId")
    @Mapping(source = "tipo", target = "tipoDescricao", qualifiedByName = "tipoToDescricao")
    HistoricoEstoqueConsolidadoResponseDTO toResponseDTO(MovimentacaoEstoqueProduto movimentacao);

    @Named("tipoToDescricao")
    static String tipoToDescricao(TipoMovimentacaoProduto tipo) {
        return tipo != null ? tipo.getDescricao() : null;
    }
}
