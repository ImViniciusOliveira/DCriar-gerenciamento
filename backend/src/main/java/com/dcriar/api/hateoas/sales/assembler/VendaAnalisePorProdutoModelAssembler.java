package com.dcriar.api.hateoas.sales.assembler;

import com.dcriar.api.controller.sales.VendaAnaliseController;
import com.dcriar.api.dto.response.sales.VendaAnalisePorProdutoItemResponseDTO;
import com.dcriar.api.hateoas.sales.model.VendaAnalisePorProdutoModel;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

@Component
public class VendaAnalisePorProdutoModelAssembler extends RepresentationModelAssemblerSupport<VendaAnalisePorProdutoItemResponseDTO, VendaAnalisePorProdutoModel> {

    public VendaAnalisePorProdutoModelAssembler() {
        super(VendaAnaliseController.class, VendaAnalisePorProdutoModel.class);
    }

    @Override
    @NonNull
    public VendaAnalisePorProdutoModel toModel(@NonNull VendaAnalisePorProdutoItemResponseDTO dto) {
        VendaAnalisePorProdutoModel model = VendaAnalisePorProdutoModel.builder()
                .produtoId(dto.getProdutoId())
                .nomeProduto(dto.getNomeProduto())
                .skuProduto(dto.getSkuProduto())
                .receita(dto.getReceita())
                .unidadesVendidas(dto.getUnidadesVendidas())
                .build();

        model.add(linkTo(VendaAnaliseController.class).withRel("analise-vendas"));

        return model;
    }
}
