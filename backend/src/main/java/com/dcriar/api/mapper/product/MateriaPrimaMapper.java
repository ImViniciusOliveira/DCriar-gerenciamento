package com.dcriar.api.mapper.product;

import com.dcriar.api.dto.response.product.MateriaPrimaResponseDTO;
import com.dcriar.domain.stock.entity.TipoMateriaPrima;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * Mapper para converter a entidade {@link TipoMateriaPrima} em um {@link MateriaPrimaResponseDTO}.
 * <p>
 * Este mapper é utilizado para aninhar as informações do tipo de matéria-prima
 * dentro de outros DTOs, como o {@link com.dcriar.api.dto.response.product.ProdutoResponseDTO}.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface MateriaPrimaMapper {

    /**
     * Converte a entidade {@link TipoMateriaPrima} para {@link MateriaPrimaResponseDTO}.
     *
     * @param tipoMateriaPrima A entidade a ser convertida.
     * @return O DTO de resposta correspondente.
     */
    MateriaPrimaResponseDTO toResponseDTO(TipoMateriaPrima tipoMateriaPrima);
}
