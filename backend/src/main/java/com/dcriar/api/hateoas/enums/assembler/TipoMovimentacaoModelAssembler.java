package com.dcriar.api.hateoas.enums.assembler;

import com.dcriar.api.controller.enums.StockEnumController;
import com.dcriar.api.hateoas.enums.model.TipoMovimentacaoModel;
import com.dcriar.domain.stock.entity.enums.TipoMovimentacao;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Assembler para converter o enum {@link TipoMovimentacao} em um modelo HATEOAS {@link TipoMovimentacaoModel}.
 * <p>
 * Este assembler é responsável por criar o modelo de representação e adicionar os links HATEOAS relevantes.
 */
@Component
public class TipoMovimentacaoModelAssembler extends RepresentationModelAssemblerSupport<TipoMovimentacao, TipoMovimentacaoModel> {

    public TipoMovimentacaoModelAssembler() {
        super(StockEnumController.class, TipoMovimentacaoModel.class);
    }

    @Override
    @NonNull
    public TipoMovimentacaoModel toModel(@NonNull TipoMovimentacao tipoMovimentacao) {
        TipoMovimentacaoModel model = instantiateModel(tipoMovimentacao);
        model.setName(tipoMovimentacao.name());

        model.add(linkTo(methodOn(StockEnumController.class).getTiposMovimentacao()).slash(tipoMovimentacao.name()).withSelfRel());

        return model;
    }
}
