package com.dcriar.api.mapper.product;

import com.dcriar.api.dto.response.product.HistoricoEstoqueConsolidadoResponseDTO;
import com.dcriar.domain.product.entity.MovimentacaoEstoqueProduto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * Mapper responsável por transformar a entidade de movimentação de estoque
 * em um DTO consolidado para a listagem histórica paginada.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface HistoricoEstoqueConsolidadoMapper {

    @Mapping(source = "produto.id", target = "produtoId")
    @Mapping(source = "produto.nome", target = "produtoNome")
    @Mapping(source = "produto.sku", target = "produtoSku")
    HistoricoEstoqueConsolidadoResponseDTO toResponseDTO(MovimentacaoEstoqueProduto movimentacao);
}
