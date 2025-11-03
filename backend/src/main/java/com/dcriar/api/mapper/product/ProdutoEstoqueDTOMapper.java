package com.dcriar.api.mapper.product;

import com.dcriar.api.dto.response.product.CanalEstoqueResponseDTO;
import com.dcriar.api.dto.response.product.ProdutoEstoqueResponseDTO;
import com.dcriar.domain.product.entity.Estoque;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * Mapper para agregar informações de estoque de um produto em vários canais.
 * <p>
 * Este mapper é projetado para uma necessidade específica do frontend, onde é preciso
 * visualizar o estoque de um produto distribuído por todos os seus canais de venda.
 */
@Mapper(componentModel = "spring", uses = CanalEstoqueDTOMapper.class)
public interface ProdutoEstoqueDTOMapper {

    /**
     * Converte um ID de produto e uma lista de seus estoques em um único {@link ProdutoEstoqueResponseDTO}.
     * <p>
     * O MapStruct utiliza o {@link CanalEstoqueDTOMapper} (declarado na anotação {@code uses})
     * para mapear cada item da lista de entidades {@link Estoque} para um {@link CanalEstoqueResponseDTO}.
     *
     * @param produtoId O ID do produto.
     * @param estoques  A lista de entidades de estoque associadas ao produto.
     * @return O DTO agregado com as informações de estoque por canal.
     */
    @Mapping(source = "produtoId", target = "produtoId")
    @Mapping(source = "estoques", target = "canais")
    ProdutoEstoqueResponseDTO toDto(Long produtoId, List<Estoque> estoques);
}
