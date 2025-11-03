package com.dcriar.api.mapper.product;

import com.dcriar.api.dto.response.product.DimensoesResponseDTO;
import com.dcriar.domain.product.entity.Dimensoes;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * Mapper para converter a entidade {@link Dimensoes} em seu DTO de resposta {@link DimensoesResponseDTO}.
 * <p>
 * Como os nomes dos campos na entidade e no DTO são idênticos (ex: larguraCm),
 * o MapStruct realiza o mapeamento automaticamente sem a necessidade de anotações @Mapping.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface DimensoesMapper {

    /**
     * Converte a entidade {@link Dimensoes} para {@link DimensoesResponseDTO}.
     *
     * @param dimensoes A entidade de dimensões a ser convertida.
     * @return O DTO de resposta correspondente.
     */
    DimensoesResponseDTO toResponseDTO(Dimensoes dimensoes);
}