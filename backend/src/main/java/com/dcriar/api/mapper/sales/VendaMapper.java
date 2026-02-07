package com.dcriar.api.mapper.sales;

import com.dcriar.api.dto.response.sales.ItemVendaResponseDTO;
import com.dcriar.api.dto.response.sales.VendaResponseDTO;
import com.dcriar.domain.sales.entity.ItemVenda;
import com.dcriar.domain.sales.entity.Venda;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * Interface MapStruct para mapear as entidades de domínio {@link Venda} e {@link ItemVenda}
 * para seus respectivos DTOs de resposta.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface VendaMapper {

    /**
     * Converte a entidade {@link Venda} para seu DTO de resposta {@link VendaResponseDTO}.
     * <p>
     * O MapStruct irá inspecionar a lista {@code items} na entidade {@code Venda} e, para cada
     * {@link ItemVenda}, invocará automaticamente o método {@link #toResponseDTO(ItemVenda)} para
     * converter a lista de itens da venda.
     *
     * @param venda A entidade de venda a ser convertida.
     * @return O DTO de resposta da venda, incluindo a lista de itens convertida.
     */
    @Mapping(source = "canalVenda.nome", target = "nomeCanalVenda")
    @Mapping(source = "canalVenda.id", target = "canalVendaId")
    VendaResponseDTO toResponseDTO(Venda venda);

    /**
     * Converte a entidade {@link ItemVenda} para seu DTO de resposta {@link ItemVendaResponseDTO}.
     * <p>
     * Este método é usado tanto diretamente quanto indiretamente pelo mapeamento de {@link Venda} para {@link VendaResponseDTO}.
     *
     * @param itemVenda A entidade de item de venda a ser convertida.
     * @return O DTO de resposta do item da venda.
     */
    @Mapping(source = "produto.id", target = "produtoId")
    @Mapping(source = "produto.sku", target = "produtoSku")
    @Mapping(source = "produto.nome", target = "nomeProduto")
    ItemVendaResponseDTO toResponseDTO(ItemVenda itemVenda);
}
