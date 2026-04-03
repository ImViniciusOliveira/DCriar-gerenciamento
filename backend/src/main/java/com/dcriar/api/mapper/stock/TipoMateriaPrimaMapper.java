package com.dcriar.api.mapper.stock;

import com.dcriar.api.dto.request.stock.TipoMateriaPrimaRequestDTO;
import com.dcriar.api.dto.response.stock.TipoMateriaPrimaResponseDTO;
import com.dcriar.api.hateoas.stock.model.TipoMateriaPrimaModel;
import com.dcriar.domain.stock.entity.TipoMateriaPrima;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

/**
 * Interface MapStruct para mapear entre a entidade {@link TipoMateriaPrima} e seus DTOs.
 * <p>
 * Abstrai a lógica de conversão, mantendo o código limpo e com baixo acoplamento.
 * A anotação {@code componentModel = "spring"} permite que o Spring gerencie
 * a implementação gerada como um Bean, facilitando a injeção de dependência.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface TipoMateriaPrimaMapper {

    /**
     * Converte um DTO de requisição {@link TipoMateriaPrimaRequestDTO} para a entidade {@link TipoMateriaPrima}.
     * <p>
     * Este método é utilizado no processo de criação de um novo tipo de matéria-prima.
     * O campo 'id' é intencionalmente ignorado, pois ele será gerado automaticamente
     * pelo banco de dados no momento da persistência e não deve ser fornecido pelo cliente.
     *
     * @param requestDTO O DTO de entrada com os dados do novo tipo de matéria-prima.
     * @return A entidade {@link TipoMateriaPrima} correspondente, pronta para ser persistida.
     */
    @Mapping(target = "id", ignore = true)
    TipoMateriaPrima toEntity(TipoMateriaPrimaRequestDTO requestDTO);

    /**
     * Converte a entidade {@link TipoMateriaPrima} para um DTO de resposta {@link TipoMateriaPrimaResponseDTO}.
     * <p>
     * Este método é utilizado em todas as operações de leitura (busca por ID, listagem)
     * para formatar os dados da entidade em um formato adequado para a camada de apresentação.
     *
     * @param tipoMateriaPrima A entidade de domínio a ser convertida.
     * @return O DTO de resposta correspondente.
     */
    @Mapping(source = "unidadeDeConsumo.descricao", target = "unidadeDescricao")
    @Mapping(target = "camposBloqueados", ignore = true)
    @Mapping(target = "motivosBloqueio", ignore = true)
    TipoMateriaPrimaResponseDTO toResponseDTO(TipoMateriaPrima tipoMateriaPrima);

    /**
     * Atualiza um modelo HATEOAS a partir de um DTO de resposta.
     * Usa @MappingTarget para evitar a criação de uma nova instância.
     *
     * @param dto O DTO de origem.
     * @param model O Modelo HATEOAS de destino a ser atualizado.
     */
    @Mapping(target = "camposBloqueados", ignore = true)
    @Mapping(target = "motivosBloqueio", ignore = true)
    void updateModelFromDto(TipoMateriaPrimaResponseDTO dto, @MappingTarget TipoMateriaPrimaModel model);
}
