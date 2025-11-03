package com.dcriar.api.mapper.product;

import com.dcriar.api.dto.response.product.CanalEstoqueResponseDTO;
import com.dcriar.domain.product.entity.Estoque;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper para converter a entidade {@link Estoque} em um {@link CanalEstoqueResponseDTO}.
 * <p>
 * Este mapper é usado para representar o estoque de um produto em um canal de venda específico.
 */
@Mapper(componentModel = "spring")
public interface CanalEstoqueDTOMapper {

    /**
     * Converte uma entidade {@link Estoque} em um DTO {@link CanalEstoqueResponseDTO}.
     *
     * @param estoque A entidade de estoque a ser convertida.
     * @return O DTO representando o estoque no canal.
     */
    @Mapping(source = "canalVenda.nome", target = "canalNome")
    @Mapping(source = "quantidade", target = "quantidade")
    CanalEstoqueResponseDTO toDto(Estoque estoque);
}
