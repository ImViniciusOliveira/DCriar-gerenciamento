package com.dcriar.api.mapper.product;

import com.dcriar.api.dto.response.product.MovimentacaoProdutoResponseDTO;
import com.dcriar.domain.product.entity.MovimentacaoEstoqueProduto;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * Interface MapStruct para mapear a entidade {@link MovimentacaoEstoqueProduto}
 * para seu DTO de resposta.
 * <p>
 * Abstrai a lógica de conversão, mantendo o código limpo e com baixo acoplamento.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface MovimentacaoProdutoMapper {

    /**
     * Converte a entidade MovimentacaoEstoqueProduto para um DTO de resposta.
     * <p>
     * O MapStruct irá mapear automaticamente os campos com nomes correspondentes.
     *
     * @param movimentacao A entidade de origem.
     * @return O DTO {@link MovimentacaoProdutoResponseDTO} correspondente.
     */
    MovimentacaoProdutoResponseDTO toResponseDTO(MovimentacaoEstoqueProduto movimentacao);
}
