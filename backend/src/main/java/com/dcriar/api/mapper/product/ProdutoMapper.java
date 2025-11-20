package com.dcriar.api.mapper.product;

import com.dcriar.api.dto.request.product.ProdutoRequestDTO;
import com.dcriar.api.dto.response.product.ProdutoResponseDTO;
import com.dcriar.api.hateoas.product.model.ProdutoModel;
import com.dcriar.domain.product.entity.Produto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * Interface MapStruct para mapear a entidade {@link Produto} para seus DTOs e Models HATEOAS.
 * <p>
 * Este mapper centraliza a lógica de conversão entre a camada de domínio (entidade)
 * e a camada de apresentação (DTOs), utilizando outros mappers como o {@link DimensoesMapper}
 * para objetos aninhados.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = {DimensoesMapper.class, MateriaPrimaMapper.class})
public interface ProdutoMapper {

    /**
     * Converte a entidade {@link Produto} para um DTO de resposta {@link ProdutoResponseDTO}.
     * <p>
     * <b>Atenção:</b> Os campos de estoque ({@code estoqueFisicoTotal}, {@code estoqueDistribuidoTotal},
     * e {@code estoqueDisponivelParaAlocar}) são intencionalmente ignorados neste mapeamento.
     * Eles são calculados e enriquecidos posteriormente na camada de serviço ({@code ProdutoServiceImpl}),
     * pois dependem de consultas adicionais ao repositório.
     *
     * @param produto A entidade de produto a ser convertida.
     * @return O DTO de resposta correspondente.
     */
    @Mapping(target = "estoqueDistribuidoTotal", ignore = true)
    @Mapping(target = "estoqueDisponivelParaAlocar", ignore = true)
    @Mapping(source = "tipoMateriaPrima", target = "materiaPrima")
    @Mapping(source = "dataCriacao", target = "dataCriacao")
    @Mapping(source = "dataAtualizacao", target = "dataAtualizacao")
    ProdutoResponseDTO toResponseDTO(Produto produto);

    /**
     * Converte um DTO de resposta {@link ProdutoResponseDTO} para o modelo de representação HATEOAS {@link ProdutoModel}.
     * <p>
     * Este método é utilizado pelo assembler HATEOAS para encapsular o DTO de resposta
     * em um modelo que pode ser enriquecido com links.
     *
     * @param responseDTO O DTO de resposta a ser convertido.
     * @return O modelo HATEOAS correspondente.
     */
    ProdutoModel toModel(ProdutoResponseDTO responseDTO);

    /**
     * Converte a entidade {@link Produto} de volta para um {@link ProdutoRequestDTO}.
     * <p>
     * Este método é útil em cenários como o PATCH, onde a entidade é modificada
     * em memória e depois precisa ser validada novamente através da estrutura de um DTO de requisição.
     *
     * @param produto A entidade de produto a ser convertida.
     * @return O DTO de requisição correspondente.
     */
    @Mapping(source = "tipoMateriaPrima.id", target = "tipoMateriaPrimaId")
    ProdutoRequestDTO toRequestDTO(Produto produto);
}
