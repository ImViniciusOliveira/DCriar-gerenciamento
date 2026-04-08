package com.dcriar.api.mapper.sales;

import com.dcriar.api.dto.response.sales.ItemVendaResponseDTO;
import com.dcriar.api.dto.response.sales.VendaResponseDTO;
import com.dcriar.api.hateoas.sales.model.VendaModel;
import com.dcriar.domain.sales.entity.ItemVenda;
import com.dcriar.domain.sales.entity.Venda;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

/**
 * Interface MapStruct para mapear as entidades de domínio {@link Venda} e {@link ItemVenda}
 * para seus respectivos DTOs de resposta.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = VendaDisplayFormatter.class)
public interface VendaMapper {

    @Mapping(source = "canalVenda.nome", target = "nomeCanalVenda")
    @Mapping(source = "canalVenda.id", target = "canalVendaId")
    @Mapping(source = "nomeCompleto", target = "nomeCompleto", qualifiedByName = "formatHumanText")
    @Mapping(source = ".", target = "pais", qualifiedByName = "formatCountry")
    @Mapping(source = "apelido", target = "apelido", qualifiedByName = "formatHumanText")
    @Mapping(source = ".", target = "cidade", qualifiedByName = "formatCity")
    @Mapping(source = ".", target = "estado", qualifiedByName = "formatState")
    @Mapping(source = ".", target = "modoLocalidade", qualifiedByName = "resolveLocationMode")
    VendaResponseDTO toResponseDTO(Venda venda);

    @Mapping(source = "canalVenda.nome", target = "nomeCanalVenda")
    @Mapping(source = "canalVenda.id", target = "canalVendaId")
    @Mapping(source = "nomeCompleto", target = "nomeCompleto", qualifiedByName = "formatHumanText")
    @Mapping(source = ".", target = "pais", qualifiedByName = "formatCountry")
    @Mapping(source = "apelido", target = "apelido", qualifiedByName = "formatHumanText")
    @Mapping(source = ".", target = "cidade", qualifiedByName = "formatCity")
    @Mapping(source = ".", target = "estado", qualifiedByName = "formatState")
    @Mapping(source = ".", target = "modoLocalidade", qualifiedByName = "resolveLocationMode")
    @Mapping(target = "endereco", ignore = true)
    @Mapping(target = "numero", ignore = true)
    @Mapping(target = "bairro", ignore = true)
    @Mapping(target = "cep", ignore = true)
    @Mapping(target = "cpf", ignore = true)
    @Mapping(target = "observacao", ignore = true)
    @Mapping(target = "itens", ignore = true)
    VendaResponseDTO toSummaryResponseDTO(Venda venda);

    @Mapping(source = "produto.id", target = "produtoId")
    @Mapping(source = "produto.sku", target = "produtoSku")
    @Mapping(source = "produto.nome", target = "nomeProduto")
    ItemVendaResponseDTO toResponseDTO(ItemVenda itemVenda);

    void updateModelFromDto(VendaResponseDTO dto, @MappingTarget VendaModel model);
}
