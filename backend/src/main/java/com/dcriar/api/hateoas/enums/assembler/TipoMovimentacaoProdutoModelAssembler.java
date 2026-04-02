package com.dcriar.api.hateoas.enums.assembler;

import com.dcriar.api.controller.enums.ProductEnumController;
import com.dcriar.api.hateoas.enums.model.TipoMovimentacaoProdutoModel;
import com.dcriar.domain.product.entity.enums.TipoMovimentacaoProduto;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Assembler para converter o enum {@link TipoMovimentacaoProduto} em um modelo HATEOAS {@link TipoMovimentacaoProdutoModel}.
 */
@Component
public class TipoMovimentacaoProdutoModelAssembler extends RepresentationModelAssemblerSupport<TipoMovimentacaoProduto, TipoMovimentacaoProdutoModel> {

    public TipoMovimentacaoProdutoModelAssembler() {
        super(ProductEnumController.class, TipoMovimentacaoProdutoModel.class);
    }

    @Override
    @NonNull
    public TipoMovimentacaoProdutoModel toModel(@NonNull TipoMovimentacaoProduto tipoMovimentacao) {
        TipoMovimentacaoProdutoModel model = instantiateModel(tipoMovimentacao);
        model.setName(tipoMovimentacao.name());
        model.setDescricao(tipoMovimentacao.getDescricao());

        String selfUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/enums/tipos-movimentacao-produto/{name}")
                .buildAndExpand(tipoMovimentacao.name())
                .toUriString();
        model.add(Link.of(selfUrl).withSelfRel());

        return model;
    }
}
