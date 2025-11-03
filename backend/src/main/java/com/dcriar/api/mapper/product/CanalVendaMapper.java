package com.dcriar.api.mapper.product;

import com.dcriar.api.dto.response.product.CanalVendaResponseDTO;
import com.dcriar.domain.product.entity.CanalVenda;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * Mapper para conversão entre a entidade CanalVenda e seus DTOs.
 */
@Mapper(componentModel = "spring")
public interface CanalVendaMapper {

    CanalVendaMapper INSTANCE = Mappers.getMapper(CanalVendaMapper.class);

    /**
     * Converte uma entidade CanalVenda para um CanalVendaResponseDTO.
     *
     * @param canalVenda A entidade a ser convertida.
     * @return O DTO de resposta correspondente.
     */
    CanalVendaResponseDTO toResponseDTO(CanalVenda canalVenda);
}
