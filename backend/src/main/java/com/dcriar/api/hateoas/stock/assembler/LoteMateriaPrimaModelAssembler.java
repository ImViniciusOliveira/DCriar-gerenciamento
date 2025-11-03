package com.dcriar.api.hateoas.stock.assembler;

import com.dcriar.api.controller.stock.LoteMateriaPrimaController;
import com.dcriar.api.controller.stock.TipoMateriaPrimaController;
import com.dcriar.api.dto.response.stock.LoteMateriaPrimaResponseDTO;
import com.dcriar.api.hateoas.stock.model.LoteMateriaPrimaModel;
import org.springframework.beans.BeanUtils;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Assembler para converter {@link LoteMateriaPrimaResponseDTO} em {@link LoteMateriaPrimaModel}
 * e construir as respostas HATEOAS para o controller.
 */
@Component
public class LoteMateriaPrimaModelAssembler extends RepresentationModelAssemblerSupport<LoteMateriaPrimaResponseDTO, LoteMateriaPrimaModel> {

    public LoteMateriaPrimaModelAssembler() {
        super(LoteMateriaPrimaController.class, LoteMateriaPrimaModel.class);
    }

    @Override
    @NonNull
    public LoteMateriaPrimaModel toModel(@NonNull LoteMateriaPrimaResponseDTO dto) {
        LoteMateriaPrimaModel model = instantiateModel(dto);
        BeanUtils.copyProperties(dto, model);

        model.add(linkTo(methodOn(LoteMateriaPrimaController.class).findById(dto.getId())).withSelfRel());
        model.add(linkTo(LoteMateriaPrimaController.class).withRel("lotes"));

        if (model.getTipoMateriaPrimaId() != null) {
            model.add(linkTo(methodOn(TipoMateriaPrimaController.class).findById(model.getTipoMateriaPrimaId())).withRel("tipo-materia-prima"));
        }

        model.add(linkTo(methodOn(LoteMateriaPrimaController.class).listarMovimentacoes(dto.getId())).withRel("movimentacoes"));

        if (model.getLoteDeOrigemId() != null) {
            model.add(linkTo(methodOn(LoteMateriaPrimaController.class).findById(model.getLoteDeOrigemId())).withRel("lote-de-origem"));
        }

        return model;
    }

    public ResponseEntity<LoteMateriaPrimaModel> toCreatedResponseEntity(@NonNull LoteMateriaPrimaResponseDTO dto) {
        LoteMateriaPrimaModel model = toModel(dto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(dto.getId())
                .toUri();
        return ResponseEntity.created(location).body(model);
    }

    public ResponseEntity<LoteMateriaPrimaModel> toOkResponseEntity(@NonNull LoteMateriaPrimaResponseDTO dto) {
        LoteMateriaPrimaModel model = toModel(dto);
        return ResponseEntity.ok(model);
    }
}
