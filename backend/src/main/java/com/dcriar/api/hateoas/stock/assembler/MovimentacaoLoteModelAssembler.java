package com.dcriar.api.hateoas.stock.assembler;

import com.dcriar.api.controller.stock.LoteMateriaPrimaController;
import com.dcriar.api.dto.response.stock.MovimentacaoResponseDTO;
import com.dcriar.api.hateoas.stock.model.MovimentacaoLoteModel;
import org.springframework.beans.BeanUtils;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Assembler para converter {@link MovimentacaoResponseDTO} em {@link MovimentacaoLoteModel}.
 */
@Component
public class MovimentacaoLoteModelAssembler extends RepresentationModelAssemblerSupport<MovimentacaoResponseDTO, MovimentacaoLoteModel> {

    public MovimentacaoLoteModelAssembler() {
        super(LoteMateriaPrimaController.class, MovimentacaoLoteModel.class);
    }

    @NonNull
    public MovimentacaoLoteModel toModel(@NonNull MovimentacaoResponseDTO dto, @NonNull Long loteId) {
        MovimentacaoLoteModel model = instantiateModel(dto);
        BeanUtils.copyProperties(dto, model);
        model.add(linkTo(methodOn(LoteMateriaPrimaController.class).listarMovimentacoes(loteId)).withRel("movimentacoes-do-lote"));
        return model;
    }

    // Este método sobrescreve o padrão para garantir que a versão com contexto seja usada.
    @Override
    @NonNull
    public MovimentacaoLoteModel toModel(@NonNull MovimentacaoResponseDTO entity) {
        throw new UnsupportedOperationException("Use o método toModel(dto, loteId) para garantir o contexto correto dos links.");
    }

    // Este método sobrescreve o padrão para adicionar o contexto do loteId.
    @Override
    @NonNull
    public CollectionModel<MovimentacaoLoteModel> toCollectionModel(@NonNull Iterable<? extends MovimentacaoResponseDTO> entities) {
        throw new UnsupportedOperationException("Use o método toCollectionModel(entities, loteId) para garantir o contexto correto dos links.");
    }

    public CollectionModel<MovimentacaoLoteModel> toCollectionModel(@NonNull Iterable<? extends MovimentacaoResponseDTO> entities, @NonNull Long loteId) {
        List<MovimentacaoLoteModel> movimentacaoModels = StreamSupport.stream(entities.spliterator(), false)
                .map(dto -> this.toModel(dto, loteId))
                .collect(Collectors.toList());

        CollectionModel<MovimentacaoLoteModel> collectionModel = CollectionModel.of(movimentacaoModels);
        collectionModel.add(linkTo(methodOn(LoteMateriaPrimaController.class).listarMovimentacoes(loteId)).withSelfRel());
        return collectionModel;
    }

    public ResponseEntity<MovimentacaoLoteModel> toCreatedResponseEntity(@NonNull MovimentacaoResponseDTO dto, @NonNull Long loteId) {
        MovimentacaoLoteModel model = toModel(dto, loteId);
        return ResponseEntity.status(201).body(model);
    }
}
