package com.dcriar.api.mapper.product;

import com.dcriar.api.dto.response.product.EstoqueResponseDTO;
import com.dcriar.domain.product.entity.Estoque;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * Interface MapStruct para mapear a entidade {@link Estoque} para seu DTO de resposta.
 * <p>
 * Abstrai a lógica de conversão, buscando dados de entidades relacionadas
 * para enriquecer a resposta da API.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface EstoqueMapper {

    /**
     * Converte a entidade Estoque para um DTO de resposta.
     * <p>
     * Usa a anotação {@code @Mapping} para buscar os dados das entidades
     * aninhadas 'produto' e 'canalVenda' e colocá-los nos campos
     * correspondentes do DTO de resposta.
     *
     * @param estoque A entidade de origem.
     * @return O DTO {@link EstoqueResponseDTO} correspondente.
     */
    @Mapping(source = "produto.id", target = "produtoId")
    @Mapping(source = "produto.nome", target = "nomeProduto")
    @Mapping(source = "canalVenda.id", target = "canalVendaId")
    @Mapping(source = "canalVenda.nome", target = "nomeCanalVenda")
    EstoqueResponseDTO toResponseDTO(Estoque estoque);
}
