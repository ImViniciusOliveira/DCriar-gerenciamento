package com.dcriar.api.mapper.production;

import com.dcriar.api.dto.response.production.CorteRealizadoResponseDTO;
import com.dcriar.domain.production.entity.CorteRealizado;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * Mapper simples para conversão entre CorteRealizado e CorteRealizadoResponseDTO.
 * Lógica de negócio (categoria de retalho, observações etc.) é executada no serviço.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CorteRealizadoMapper {
    @Mapping(source = "ordemDeProducao.id", target = "ordemDeProducaoId")
    CorteRealizadoResponseDTO toResponseDTO(CorteRealizado entity);
}