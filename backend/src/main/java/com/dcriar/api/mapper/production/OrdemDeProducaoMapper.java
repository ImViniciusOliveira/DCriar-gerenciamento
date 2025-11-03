package com.dcriar.api.mapper.production;

import com.dcriar.api.dto.request.production.MargensRequestDTO;
import com.dcriar.api.dto.response.production.CorteRealizadoResponseDTO;
import com.dcriar.api.dto.response.production.OrdemDeProducaoResponseDTO;
import com.dcriar.api.hateoas.production.model.OrdemDeProducaoModel;
import com.dcriar.domain.production.entity.CorteRealizado;
import com.dcriar.domain.production.entity.Margens;
import com.dcriar.domain.production.entity.OrdemDeProducao;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Interface MapStruct para mapear a entidade {@link OrdemDeProducao} para seus DTOs e Models HATEOAS.
 * <p>
 * Centraliza a conversão entre a camada de domínio e a camada de apresentação para Ordens de Produção.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface OrdemDeProducaoMapper {

    /**
     * Converte a entidade {@link OrdemDeProducao} para um DTO de resposta {@link OrdemDeProducaoResponseDTO}.
     * <p>
     * Mapeia campos de entidades relacionadas (como Produto) e utiliza um método qualificado
     * ({@code lotesToIds}) para converter o conjunto de entidades de lote em uma lista de IDs.
     *
     * @param ordem A entidade de Ordem de Produção a ser convertida.
     * @return O DTO de resposta correspondente.
     */
    @Mapping(target = "detalhesCorte", ignore = true) // Este campo é montado separadamente no serviço, se aplicável.
    @Mapping(source = "produto.id", target = "produtoId")
    @Mapping(source = "produto.nome", target = "nomeProduto")
    @Mapping(source = "lotesConsumidos", target = "lotesConsumidosIds", qualifiedByName = "lotesToIds")
    @Mapping(source = "cortesRealizados", target = "cortesRealizados")
    OrdemDeProducaoResponseDTO toDto(OrdemDeProducao ordem);

    /**
     * Converte um DTO de resposta {@link OrdemDeProducaoResponseDTO} para o modelo de representação HATEOAS {@link OrdemDeProducaoModel}.
     * <p>
     * Como os nomes dos campos são idênticos, o MapStruct realiza o mapeamento automaticamente.
     * Este método é utilizado pelo assembler HATEOAS para encapsular o DTO antes de adicionar os links.
     *
     * @param dto O DTO de resposta a ser convertido.
     * @return O Modelo HATEOAS correspondente.
     */
    OrdemDeProducaoModel toModel(OrdemDeProducaoResponseDTO dto);

    /**
     * Converte um DTO de requisição de margens {@link MargensRequestDTO} para a entidade embutível {@link Margens}.
     * <p>
     * Conversão simples, sem lógica de negócio. Usado apenas para mapear dados entre camadas.
     *
     * @param dto O DTO de requisição contendo os valores das margens.
     * @return A entidade {@link Margens} preenchida.
     */
    Margens toMargensEntity(MargensRequestDTO dto);

    /**
     * Converte a entidade {@link CorteRealizado} para seu DTO de resposta.
     * O MapStruct usa este método para mapear a lista de cortes dentro do {@code toDto(OrdemDeProducao)}.
     *
     * @param corte A entidade de corte a ser convertida.
     * @return O DTO de resposta correspondente.
     */
    @Mapping(source = "ordemDeProducao.id", target = "ordemDeProducaoId")
    @Mapping(source = "retalhoCategoria", target = "retalhoCategoria")
    CorteRealizadoResponseDTO toCorteDto(CorteRealizado corte);

    /**
     * Método auxiliar para converter um conjunto de LoteMateriaPrima em uma lista de IDs.
     *
     * @param lotes O conjunto de LoteMateriaPrima a ser convertido.
     * @return Uma lista de IDs correspondente aos LoteMateriaPrima fornecidos.
     */
    @Named("lotesToIds")
    static List<Long> lotesToIds(Set<com.dcriar.domain.stock.entity.LoteMateriaPrima> lotes) {
        if (lotes == null) return null;
        return lotes.stream().map(com.dcriar.domain.stock.entity.LoteMateriaPrima::getId).collect(Collectors.toList());
    }
}
