package com.dcriar.api.hateoas.product.assembler;

import com.dcriar.api.controller.product.EstoqueProdutoController;
import com.dcriar.api.controller.product.ProdutoController;
import com.dcriar.api.dto.response.product.EstoqueResponseDTO;
import com.dcriar.api.hateoas.product.model.EstoqueProdutoModel;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Assembler responsável por converter {@link EstoqueResponseDTO} em {@link EstoqueProdutoModel}
 * e adicionar os links HATEOAS apropriados.
 */
@Component
public class EstoqueProdutoModelAssembler extends RepresentationModelAssemblerSupport<EstoqueResponseDTO, EstoqueProdutoModel> {

    public EstoqueProdutoModelAssembler() {
        super(EstoqueProdutoController.class, EstoqueProdutoModel.class);
    }

    @Override
    @NonNull
    public EstoqueProdutoModel toModel(@NonNull EstoqueResponseDTO dto) {
        EstoqueProdutoModel model = EstoqueProdutoModel.fromDto(dto);

        model.add(linkTo(methodOn(EstoqueProdutoController.class)
                .listarConsultasEstoque(
                        model.getProdutoId(),
                        null,
                        model.getCanalVendaId(),
                        false,
                        PageRequest.of(0, 10, Sort.by("produto.nome").ascending()),
                        null
                ))
                .withSelfRel());
        model.add(linkTo(methodOn(EstoqueProdutoController.class).ajustarEstoqueCanal(null)).withRel("ajustar-estoque-canal"));
        model.add(linkTo(methodOn(ProdutoController.class).findById(model.getProdutoId())).withRel("produto"));

        return model;
    }

    public CollectionModel<EstoqueProdutoModel> toCollectionModel(List<EstoqueResponseDTO> entities, Long produtoId) {
        List<EstoqueProdutoModel> estoqueModels = entities.stream()
                .map(this::toModel)
                .collect(Collectors.toList());

        CollectionModel<EstoqueProdutoModel> collectionModel = CollectionModel.of(estoqueModels);

        collectionModel.add(linkTo(methodOn(EstoqueProdutoController.class).ajustarEstoqueFisico(null)).withRel("ajustar-estoque-fisico"));
        collectionModel.add(linkTo(methodOn(EstoqueProdutoController.class)
                .listarHistoricoPorProduto(produtoId, "all", null, PageRequest.of(0, 10, Sort.by("data").descending()), null))
                .withRel("historico-do-produto"));
        collectionModel.add(linkTo(methodOn(ProdutoController.class).findById(produtoId)).withRel("produto"));

        return collectionModel;
    }

    /**
     * Constrói a resposta HTTP completa para uma consulta bem-sucedida de um recurso.
     *
     * @param dto O DTO do recurso consultado.
     * @return Um ResponseEntity<EstoqueProdutoModel> com status 200 e corpo HATEOAS.
     */
    public ResponseEntity<EstoqueProdutoModel> toOkResponseEntity(@NonNull EstoqueResponseDTO dto) {
        EstoqueProdutoModel model = toModel(dto);
        return ResponseEntity.ok(model);
    }

    /**
     * Constrói a resposta HTTP completa para uma consulta bem-sucedida de uma coleção de recursos.
     *
     * @param entities A lista de DTOs da coleção.
     * @param produtoId O ID do produto para construir os links da coleção.
     * @return Um ResponseEntity<CollectionModel<EstoqueProdutoModel>> com status 200 e corpo HATEOAS.
     */
    public ResponseEntity<CollectionModel<EstoqueProdutoModel>> toOkResponseEntity(List<EstoqueResponseDTO> entities, Long produtoId) {
        CollectionModel<EstoqueProdutoModel> collectionModel = toCollectionModel(entities, produtoId);
        return ResponseEntity.ok(collectionModel);
    }
}
