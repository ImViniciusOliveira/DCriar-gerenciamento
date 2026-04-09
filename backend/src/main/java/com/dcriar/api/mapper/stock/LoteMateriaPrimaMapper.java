package com.dcriar.api.mapper.stock;

import com.dcriar.api.dto.response.stock.LoteMateriaPrimaResponseDTO;
import com.dcriar.api.hateoas.stock.model.LoteMateriaPrimaModel;
import com.dcriar.domain.stock.entity.LoteMateriaPrima;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

/**
 * Interface MapStruct para mapear a entidade {@link LoteMateriaPrima} para seu DTO de resposta {@link LoteMateriaPrimaResponseDTO}.
 * <p>
 * Este mapper centraliza a lógica de conversão, extraindo informações da entidade
 * e de suas associações (como {@link com.dcriar.domain.stock.entity.TipoMateriaPrima})
 * para popular o DTO de resposta.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface LoteMateriaPrimaMapper {

    /**
     * Converte a entidade {@link LoteMateriaPrima} para um DTO de resposta {@link LoteMateriaPrimaResponseDTO}.
     * <p>
     * <b>Atenção:</b> O campo {@code saldoEstoque} é intencionalmente ignorado neste mapeamento.
     * Ele é calculado e enriquecido posteriormente na camada de serviço ({@code LoteMateriaPrimaServiceImpl}),
     * pois depende de uma consulta agregada ao histórico de movimentações.
     *
     * @param lote A entidade de lote de matéria-prima a ser convertida.
     * @return O DTO de resposta correspondente, sem o saldo de estoque preenchido.
     */
    @Mapping(source = "tipoMateriaPrima.id", target = "tipoMateriaPrimaId")
    @Mapping(source = "tipoMateriaPrima.nome", target = "nomeTipoMateriaPrima")
    @Mapping(source = "loteDeOrigem.id", target = "loteDeOrigemId")
    @Mapping(source = "unidadeDeEstoque.simbolo", target = "unidadeSimbolo")
    @Mapping(target = "saldoEstoque", ignore = true)
    @Mapping(target = "saldoInternoAtual", ignore = true)
    @Mapping(target = "valorAtualLote", ignore = true)
    @Mapping(target = "custoUnitarioAtual", ignore = true)
    @Mapping(target = "identificadorPublico", ignore = true)
    @Mapping(target = "identificadorOrigemPublico", ignore = true)
    @Mapping(target = "tipoEstrutural", ignore = true)
    @Mapping(target = "camposBloqueados", ignore = true)
    @Mapping(target = "motivosBloqueio", ignore = true)
    LoteMateriaPrimaResponseDTO toResponseDTO(LoteMateriaPrima lote);

    /**
     * Atualiza um modelo HATEOAS a partir de um DTO de resposta.
     * Usa @MappingTarget para evitar a criação de uma nova instância.
     *
     * @param dto O DTO de origem.
     * @param model O Modelo HATEOAS de destino a ser atualizado.
     */
    @Mapping(target = "saldoInternoAtual", ignore = true)
    @Mapping(target = "valorAtualLote", ignore = true)
    @Mapping(target = "custoUnitarioAtual", ignore = true)
    void updateModelFromDto(LoteMateriaPrimaResponseDTO dto, @MappingTarget LoteMateriaPrimaModel model);
}
