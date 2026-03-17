package com.dcriar.api.hateoas.production.assembler;

import com.dcriar.api.controller.production.OrdemDeProducaoController;
import com.dcriar.api.dto.response.production.OrdemDeConsumoResponseDTO;
import com.dcriar.api.hateoas.production.model.OrdemDeConsumoModel;
import org.springframework.beans.BeanUtils;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@Component
public class OrdemDeConsumoModelAssembler extends RepresentationModelAssemblerSupport<OrdemDeConsumoResponseDTO, OrdemDeConsumoModel> {

    public OrdemDeConsumoModelAssembler() {
        super(OrdemDeProducaoController.class, OrdemDeConsumoModel.class);
    }

    @Override
    @NonNull
    public OrdemDeConsumoModel toModel(@NonNull OrdemDeConsumoResponseDTO dto) {
        OrdemDeConsumoModel model = instantiateModel(dto);
        BeanUtils.copyProperties(dto, model);

        if (dto.getId() != null) {
            String selfUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/v1/ordens-de-producao/{id}")
                    .buildAndExpand(dto.getId())
                    .toUriString();
            model.add(Link.of(selfUrl).withSelfRel());
            model.add(Link.of(selfUrl, "deletar-ordem-de-producao"));
        }

        String colecaoUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/ordens-de-producao")
                .toUriString();
        model.add(Link.of(colecaoUrl, "ordens-de-producao"));

        return model;
    }

    public ResponseEntity<OrdemDeConsumoModel> toCreatedResponseEntity(@NonNull OrdemDeConsumoResponseDTO dto) {
        OrdemDeConsumoModel model = toModel(dto);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/ordens-de-producao/{id}")
                .buildAndExpand(dto.getId())
                .toUri();
        return ResponseEntity.created(location).body(model);
    }
}
