package com.dcriar.api.hateoas.enums.assembler;

import com.dcriar.api.controller.enums.ProductEnumController;
import com.dcriar.api.hateoas.enums.model.TipoPrecoModel;
import com.dcriar.domain.product.entity.enums.TipoPreco;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Assembler para converter o enum {@link TipoPreco} em um modelo HATEOAS {@link TipoPrecoModel}.
 * <p>
 * Este assembler é responsável por criar o modelo de representação e adicionar os links HATEOAS relevantes.
 */
@Component
public class TipoPrecoModelAssembler extends RepresentationModelAssemblerSupport<TipoPreco, TipoPrecoModel> {

    public TipoPrecoModelAssembler() {
        super(ProductEnumController.class, TipoPrecoModel.class);
    }

    @Override
    @NonNull
    public TipoPrecoModel toModel(@NonNull TipoPreco tipoPreco) {
        TipoPrecoModel model = instantiateModel(tipoPreco);
        model.setName(tipoPreco.name());

        // Adiciona o link para o próprio recurso (self link).
        model.add(linkTo(methodOn(ProductEnumController.class).getTiposPreco()).slash(tipoPreco.name()).withSelfRel());

        return model;
    }
}
