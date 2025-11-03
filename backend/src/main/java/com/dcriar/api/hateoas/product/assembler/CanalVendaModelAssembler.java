package com.dcriar.api.hateoas.product.assembler;

import com.dcriar.api.controller.product.CanalVendaController;
import com.dcriar.api.dto.response.product.CanalVendaResponseDTO;
import com.dcriar.api.hateoas.enums.model.CanalVendaModel;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Assembler para converter CanalVendaResponseDTO em CanalVendaModel (HATEOAS).
 */
@Component
public class CanalVendaModelAssembler extends RepresentationModelAssemblerSupport<CanalVendaResponseDTO, CanalVendaModel> {

    public CanalVendaModelAssembler() {
        super(CanalVendaController.class, CanalVendaModel.class);
    }

    @Override
    @NonNull
    public CanalVendaModel toModel(@NonNull CanalVendaResponseDTO dto) {
        CanalVendaModel model = new CanalVendaModel(dto);

        model.add(linkTo(methodOn(CanalVendaController.class).findById(dto.getId())).withSelfRel());
        model.add(linkTo(methodOn(CanalVendaController.class).findAll()).withRel("canais-venda"));

        return model;
    }

    /**
     * Constrói um ResponseEntity para um recurso recém-criado (HTTP 201 Created).
     *
     * @param dto O DTO do recurso criado.
     * @return Um ResponseEntity com o status 201, o link para o recurso no cabeçalho Location e o modelo HATEOAS no corpo.
     */
    public ResponseEntity<CanalVendaModel> toCreatedResponseEntity(@NonNull CanalVendaResponseDTO dto) {
        CanalVendaModel model = toModel(dto);
        return ResponseEntity
                .created(model.getRequiredLink("self").toUri())
                .body(model);
    }
}
