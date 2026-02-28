package com.dcriar.api.hateoas.enums.assembler;

import com.dcriar.api.controller.enums.ProductionEnumController;
import com.dcriar.api.hateoas.enums.model.ModoCalculoModel;
import com.dcriar.domain.production.enums.ModoCalculo;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

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

        String selfUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/enums/modos-calculo/{name}")
                .buildAndExpand(modoCalculo.name())
                .toUriString();
        model.add(Link.of(selfUrl).withSelfRel());

        return model;
    }
}
