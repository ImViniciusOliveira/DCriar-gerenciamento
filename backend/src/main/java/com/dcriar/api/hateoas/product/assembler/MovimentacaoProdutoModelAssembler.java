package com.dcriar.api.hateoas.product.assembler;

import com.dcriar.api.controller.product.EstoqueProdutoController;
import com.dcriar.api.controller.product.ProdutoController;
import com.dcriar.api.dto.response.product.MovimentacaoProdutoResponseDTO;
import com.dcriar.api.hateoas.product.model.MovimentacaoProdutoModel;
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
 * Assembler responsável por converter {@link MovimentacaoProdutoResponseDTO} em {@link MovimentacaoProdutoModel}
 * e adicionar os links HATEOAS apropriados.
 */
@Component
public class MovimentacaoProdutoModelAssembler extends RepresentationModelAssemblerSupport<MovimentacaoProdutoResponseDTO, MovimentacaoProdutoModel> {

    public MovimentacaoProdutoModelAssembler() {
        super(EstoqueProdutoController.class, MovimentacaoProdutoModel.class);
    }

    @NonNull
    public MovimentacaoProdutoModel toModel(@NonNull MovimentacaoProdutoResponseDTO dto, @NonNull Long produtoId) {
        MovimentacaoProdutoModel model = MovimentacaoProdutoModel.fromDto(dto);
        model.add(linkTo(methodOn(EstoqueProdutoController.class).listarMovimentacoesPorProduto(produtoId)).withRel("historico-do-produto"));
        return model;
    }

    public CollectionModel<MovimentacaoProdutoModel> toCollectionModel(List<MovimentacaoProdutoResponseDTO> entities, Long produtoId) {
        List<MovimentacaoProdutoModel> movimentacaoModels = entities.stream()
                .map(dto -> this.toModel(dto, produtoId))
                .collect(Collectors.toList());

        CollectionModel<MovimentacaoProdutoModel> collectionModel = CollectionModel.of(movimentacaoModels);

        collectionModel.add(linkTo(methodOn(EstoqueProdutoController.class).listarMovimentacoesPorProduto(produtoId)).withSelfRel());
        collectionModel.add(linkTo(methodOn(ProdutoController.class).findById(produtoId)).withRel("produto"));
        collectionModel.add(linkTo(methodOn(EstoqueProdutoController.class).listarEstoquesPorProduto(produtoId)).withRel("estoques-do-produto"));
        collectionModel.add(linkTo(methodOn(EstoqueProdutoController.class).ajustarEstoqueFisico(null)).withRel("ajustar-estoque-fisico"));

        return collectionModel;
    }

    /**
     * Constrói a resposta HTTP completa para uma consulta bem-sucedida de uma coleção de movimentações.
     *
     * @param entities A lista de DTOs da coleção.
     * @param produtoId O ID do produto para construir os links da coleção.
     * @return Um ResponseEntity<CollectionModel<MovimentacaoProdutoModel>> com status 200 e corpo HATEOAS.
     */
    public ResponseEntity<CollectionModel<MovimentacaoProdutoModel>> toOkResponseEntity(List<MovimentacaoProdutoResponseDTO> entities, Long produtoId) {
        CollectionModel<MovimentacaoProdutoModel> collectionModel = toCollectionModel(entities, produtoId);
        return ResponseEntity.ok(collectionModel);
    }

    @Override
    @NonNull
    public MovimentacaoProdutoModel toModel(@NonNull MovimentacaoProdutoResponseDTO entity) {
        throw new UnsupportedOperationException("Use o método toModel(dto, produtoId) para garantir o contexto correto dos links.");
    }
}
