package com.dcriar.api.hateoas.stock.assembler;

import com.dcriar.api.controller.stock.LoteMateriaPrimaController;
import com.dcriar.api.controller.stock.TipoMateriaPrimaController;
import com.dcriar.api.dto.response.stock.TipoMateriaPrimaResponseDTO;
import com.dcriar.api.hateoas.stock.model.TipoMateriaPrimaModel;
import org.springframework.beans.BeanUtils;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Assembler para converter {@link TipoMateriaPrimaResponseDTO} em {@link TipoMateriaPrimaModel}
 * e construir as respostas HATEOAS para o controller.
 */
@Component
public class TipoMateriaPrimaModelAssembler extends RepresentationModelAssemblerSupport<TipoMateriaPrimaResponseDTO, TipoMateriaPrimaModel> {

    public TipoMateriaPrimaModelAssembler() {
        super(TipoMateriaPrimaController.class, TipoMateriaPrimaModel.class);
    }

    @Override
    @NonNull
    public TipoMateriaPrimaModel toModel(@NonNull TipoMateriaPrimaResponseDTO dto) {
        TipoMateriaPrimaModel model = instantiateModel(dto);
        BeanUtils.copyProperties(dto, model);

        // Link para o próprio recurso
        model.add(linkTo(methodOn(TipoMateriaPrimaController.class).findById(dto.getId())).withSelfRel());
        // Link para a coleção de todos os tipos
        model.add(linkTo(methodOn(TipoMateriaPrimaController.class).findAll()).withRel(IanaLinkRelations.COLLECTION));

        // Link para o recurso relacionado: listar todos os lotes deste tipo
        String lotesUri = linkTo(LoteMateriaPrimaController.class).toUri().toString();
        Link lotesLink = Link.of(UriComponentsBuilder.fromUriString(lotesUri)
                .queryParam("tipoMateriaPrimaId", dto.getId()).build().toUriString(), "lotes");
        model.add(lotesLink);

        return model;
    }

    public ResponseEntity<TipoMateriaPrimaModel> toCreatedResponseEntity(@NonNull TipoMateriaPrimaResponseDTO dto) {
        TipoMateriaPrimaModel model = toModel(dto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(dto.getId())
                .toUri();
        return ResponseEntity.created(location).body(model);
    }

    public ResponseEntity<TipoMateriaPrimaModel> toOkResponseEntity(@NonNull TipoMateriaPrimaResponseDTO dto) {
        TipoMateriaPrimaModel model = toModel(dto);
        return ResponseEntity.ok(model);
    }
}
