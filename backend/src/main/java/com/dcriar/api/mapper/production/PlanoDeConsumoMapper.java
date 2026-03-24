package com.dcriar.api.mapper.production;

import com.dcriar.api.dto.response.production.PlanoDeConsumoItemDTO;
import com.dcriar.domain.production.model.PlanoDeConsumoItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PlanoDeConsumoMapper {

    @Mapping(source = "lote.id", target = "loteId")
    @Mapping(source = "lote.motivo", target = "motivoLote")
    @Mapping(
            target = "quantidadeAConsumir",
            expression = "java(item.lote().getTipoMateriaPrima().getUnidadeDeConsumo().converterQuantidadeDaUnidadeInternaParaInformada(item.quantidadeAConsumir(), item.lote().getTipoMateriaPrima().getUnidadeDeConsumo()))"
    )
    PlanoDeConsumoItemDTO toDto(PlanoDeConsumoItem item);
}
