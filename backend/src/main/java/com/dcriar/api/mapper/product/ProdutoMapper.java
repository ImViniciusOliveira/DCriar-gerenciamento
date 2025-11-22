package com.dcriar.api.mapper.product;

import com.dcriar.api.dto.response.product.ProdutoDeConsumoDiretoResponseDTO;
import com.dcriar.api.dto.response.product.ProdutoDeCorteResponseDTO;
import com.dcriar.api.dto.response.product.ProdutoResponseDTO;
import com.dcriar.api.hateoas.product.model.ProdutoDeConsumoDiretoModel;
import com.dcriar.api.hateoas.product.model.ProdutoDeCorteModel;
import com.dcriar.api.hateoas.product.model.ProdutoModel;
import com.dcriar.domain.product.entity.Produto;
import com.dcriar.domain.product.entity.ProdutoDeConsumoDireto;
import com.dcriar.domain.product.entity.ProdutoDeCorte;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
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
     * @return O DTO de resposta correspondente ({@link ProdutoDeCorteResponseDTO} ou {@link ProdutoDeConsumoDiretoResponseDTO}).
     */
    default ProdutoResponseDTO toResponseDTO(Produto produto) {
        if (produto instanceof ProdutoDeCorte produtoDeCorte) {
            return toCorteResponseDTO(produtoDeCorte);
        } else if (produto instanceof ProdutoDeConsumoDireto produtoDeConsumoDireto) {
            return toConsumoDiretoResponseDTO(produtoDeConsumoDireto);
        }
        throw new IllegalArgumentException("Tipo de produto desconhecido: " + produto.getClass().getName());
    }

    @Mapping(target = "estoqueDistribuidoTotal", ignore = true)
    @Mapping(target = "estoqueDisponivelParaAlocar", ignore = true)
    @Mapping(source = "tipoMateriaPrima", target = "materiaPrima")
    @Mapping(target = "tipoProduto", constant = "CORTE")
    ProdutoDeCorteResponseDTO toCorteResponseDTO(ProdutoDeCorte produto);

    @Mapping(target = "estoqueDistribuidoTotal", ignore = true)
    @Mapping(target = "estoqueDisponivelParaAlocar", ignore = true)
    @Mapping(source = "tipoMateriaPrima", target = "materiaPrima")
    @Mapping(target = "tipoProduto", constant = "CONSUMO_DIRETO")
    ProdutoDeConsumoDiretoResponseDTO toConsumoDiretoResponseDTO(ProdutoDeConsumoDireto produto);

    /**
     * Converte um DTO de resposta {@link ProdutoResponseDTO} para o modelo HATEOAS apropriado.
     * <p>
     * Devido à incompatibilidade entre o {@code @SuperBuilder} do Lombok e a classe {@link org.springframework.hateoas.RepresentationModel},
     * a instanciação e o mapeamento são feitos manualmente usando {@link BeanUtils#copyProperties(Object, Object)}.
     *
     * @param dto O DTO de resposta a ser convertido.
     * @return O modelo HATEOAS correspondente ({@link ProdutoDeCorteModel} ou {@link ProdutoDeConsumoDiretoModel}).
     */
    default ProdutoModel toModel(ProdutoResponseDTO dto) {
        if (dto instanceof ProdutoDeCorteResponseDTO corteDto) {
            ProdutoDeCorteModel model = new ProdutoDeCorteModel();
            BeanUtils.copyProperties(corteDto, model);
            return model;
        } else if (dto instanceof ProdutoDeConsumoDiretoResponseDTO consumoDto) {
            ProdutoDeConsumoDiretoModel model = new ProdutoDeConsumoDiretoModel();
            BeanUtils.copyProperties(consumoDto, model);
            return model;
        }
        throw new IllegalArgumentException("Tipo de DTO de produto desconhecido: " + dto.getClass().getName());
    }
}
