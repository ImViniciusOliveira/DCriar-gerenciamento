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
    @Mapping(target = "estoqueFisicoTotal", ignore = true)
    @Mapping(target = "estoqueDistribuidoTotal", ignore = true)
    @Mapping(target = "estoqueDisponivelParaAlocar", ignore = true)
    @Mapping(source = "tipoMateriaPrima", target = "materiaPrima")
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
     * Converte um DTO de resposta {@link ProdutoResponseDTO} de volta para um DTO de requisição {@link ProdutoRequestDTO}.
     * <p>
     * Este método é essencial para a implementação de operações de atualização parcial (PATCH).
     * Ele permite obter o estado atual de um recurso (como um DTO de resposta), convertê-lo
     * para um DTO de requisição, aplicar as alterações parciais sobre ele e, em seguida,
     * proceder com a validação e persistência.
     *
     * @param responseDTO O DTO de resposta que representa o estado atual do produto.
     * @return Um DTO de requisição pronto para ser mesclado e validado.
     */
    @Mapping(source = "materiaPrima.id", target = "tipoMateriaPrimaId")
    ProdutoRequestDTO toRequestDTO(ProdutoResponseDTO responseDTO);
}