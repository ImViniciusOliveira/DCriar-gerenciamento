package com.dcriar.api.mapper.product;

import com.dcriar.api.dto.response.product.ProdutoDeConsumoResponseDTO;
import com.dcriar.api.dto.response.product.ProdutoDeCorteResponseDTO;
import com.dcriar.api.dto.response.product.ProdutoResponseDTO;
import com.dcriar.api.hateoas.product.model.ProdutoDeConsumoModel;
import com.dcriar.api.hateoas.product.model.ProdutoDeCorteModel;
import com.dcriar.api.hateoas.product.model.ProdutoModel;
import com.dcriar.domain.product.entity.Produto;
import com.dcriar.domain.product.entity.ProdutoDeConsumo;
import com.dcriar.domain.product.entity.ProdutoDeCorte;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.springframework.beans.BeanUtils;

/**
 * Mapper polimórfico para a hierarquia de Produtos.
 * <p>
 * Este mapper é responsável por converter as entidades de produto ({@link Produto}, {@link ProdutoDeCorte}, etc.)
 * para seus respectivos DTOs de resposta, e estes para os modelos HATEOAS.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = {DimensoesMapper.class, MateriaPrimaMapper.class})
public interface ProdutoMapper {

    /**
     * Converte uma entidade {@link Produto} para o DTO de resposta apropriado.
     * <p>
     * Utiliza "pattern matching for instanceof" para determinar o tipo real da entidade
     * e delegar a conversão para o método de mapeamento específico da subclasse.
     *
     * @param produto A entidade de produto a ser convertida.
     * @return O DTO de resposta correspondente ({@link ProdutoDeCorteResponseDTO} ou {@link ProdutoDeConsumoResponseDTO}).
     */
    default ProdutoResponseDTO toResponseDTO(Produto produto) {
        if (produto instanceof ProdutoDeCorte produtoDeCorte) {
            return toCorteResponseDTO(produtoDeCorte);
        } else if (produto instanceof ProdutoDeConsumo produtoDeConsumo) {
            return toConsumoResponseDTO(produtoDeConsumo);
        }
        throw new IllegalArgumentException("Tipo de produto desconhecido: " + produto.getClass().getName());
    }

    @Mapping(target = "estoqueDistribuidoTotal", ignore = true)
    @Mapping(target = "estoqueDisponivelParaAlocar", ignore = true)
    @Mapping(target = "camposBloqueados", ignore = true)
    @Mapping(target = "motivosBloqueio", ignore = true)
    @Mapping(source = "tipoMateriaPrima", target = "materiaPrima")
    @Mapping(target = "tipoProduto", constant = "CORTE")
    ProdutoDeCorteResponseDTO toCorteResponseDTO(ProdutoDeCorte produto);

    @Mapping(target = "estoqueDistribuidoTotal", ignore = true)
    @Mapping(target = "estoqueDisponivelParaAlocar", ignore = true)
    @Mapping(target = "camposBloqueados", ignore = true)
    @Mapping(target = "motivosBloqueio", ignore = true)
    @Mapping(source = "tipoMateriaPrima", target = "materiaPrima")
    @Mapping(target = "tipoProduto", constant = "CONSUMO")
    ProdutoDeConsumoResponseDTO toConsumoResponseDTO(ProdutoDeConsumo produto);

    /**
     * Atualiza um modelo HATEOAS a partir de um DTO de resposta, lidando com polimorfismo.
     * <p>
     * Usa @MappingTarget para evitar a criação de uma nova instância, quebrando dependências circulares.
     * A cópia das propriedades é feita via {@link BeanUtils#copyProperties(Object, Object)} para
     * compatibilizar com a hierarquia de classes dos modelos.
     *
     * @param dto O DTO de origem.
     * @param model O Modelo HATEOAS de destino a ser atualizado.
     */
    default void updateModelFromDto(ProdutoResponseDTO dto, @MappingTarget ProdutoModel model) {
        if (dto instanceof ProdutoDeCorteResponseDTO && model instanceof ProdutoDeCorteModel) {
            BeanUtils.copyProperties(dto, model);
        } else if (dto instanceof ProdutoDeConsumoResponseDTO && model instanceof ProdutoDeConsumoModel) {
            BeanUtils.copyProperties(dto, model);
        } else {
            // Fallback para o caso geral ou se os tipos não corresponderem exatamente
            BeanUtils.copyProperties(dto, model);
        }
    }
}
