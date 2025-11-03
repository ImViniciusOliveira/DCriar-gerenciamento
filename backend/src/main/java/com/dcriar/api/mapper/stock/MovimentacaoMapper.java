package com.dcriar.api.mapper.stock;

import com.dcriar.api.dto.response.stock.MovimentacaoResponseDTO;
import com.dcriar.domain.stock.entity.MovimentacaoEstoqueLote;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * Interface MapStruct para mapear a entidade {@link MovimentacaoEstoqueLote}
 * para seu DTO de resposta, {@link MovimentacaoResponseDTO}.
 * <p>
 * Este mapper é responsável por converter o registro de histórico de movimentação de um lote
 * em um formato adequado para a camada de apresentação.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface MovimentacaoMapper {

    /**
     * Converte a entidade {@link MovimentacaoEstoqueLote} para um DTO de resposta {@link MovimentacaoResponseDTO}.
     * <p>
     * O MapStruct irá mapear automaticamente todos os campos com nomes correspondentes
     * (ex: {@code data}, {@code tipo}, {@code quantidade}, {@code motivo}, {@code custoPorUnidadeBase}).
     *
     * @param movimentacao A entidade de movimentação de estoque do lote a ser convertida.
     * @return O DTO de resposta correspondente.
     */
    MovimentacaoResponseDTO toResponseDTO(MovimentacaoEstoqueLote movimentacao);
}