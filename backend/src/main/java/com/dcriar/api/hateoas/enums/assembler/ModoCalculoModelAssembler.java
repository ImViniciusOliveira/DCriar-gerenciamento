package com.dcriar.api.hateoas.enums.assembler;

import com.dcriar.api.controller.enums.ProductionEnumController;
import com.dcriar.api.hateoas.enums.model.ModoCalculoModel;
import com.dcriar.domain.production.enums.ModoCalculo;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Assembler para converter o enum {@link ModoCalculo} em um modelo HATEOAS {@link ModoCalculoModel}.
 */
@Component
public class ModoCalculoModelAssembler extends RepresentationModelAssemblerSupport<ModoCalculo, ModoCalculoModel> {

    public ModoCalculoModelAssembler() {
        super(ProductionEnumController.class, ModoCalculoModel.class);
    }

    @Override
    @NonNull
    public ModoCalculoModel toModel(@NonNull ModoCalculo modoCalculo) {
        ModoCalculoModel model = instantiateModel(modoCalculo);
        model.setName(modoCalculo.name());

        model.add(linkTo(methodOn(ProductionEnumController.class).getModosCalculo()).slash(modoCalculo.name()).withSelfRel());

        return model;
    }
}
