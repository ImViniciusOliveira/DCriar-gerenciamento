package com.dcriar.api.hateoas.sales.assembler;

import com.dcriar.api.controller.product.ProdutoController;
import com.dcriar.api.controller.sales.VendaController;
import com.dcriar.api.dto.response.sales.ItemVendaResponseDTO;
import com.dcriar.api.hateoas.sales.model.ItemVendaModel;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Assembler responsável por converter {@link ItemVendaResponseDTO} em {@link ItemVendaModel}
 * e adicionar os links HATEOAS apropriados para um item de venda.
 */
@Component
public class ItemVendaModelAssembler extends RepresentationModelAssemblerSupport<ItemVendaResponseDTO, ItemVendaModel> {

    public ItemVendaModelAssembler() {
        super(VendaController.class, ItemVendaModel.class);
    }

    @Override
    @NonNull
    public ItemVendaModel toModel(@NonNull ItemVendaResponseDTO dto) {
        ItemVendaModel model = ItemVendaModel.fromResponseDTO(dto);

        // Adiciona um link para o recurso do produto associado a este item de venda.
        if (model.getProdutoId() != null) {
            model.add(linkTo(methodOn(ProdutoController.class).findById(model.getProdutoId())).withRel("produto"));
        }

        return model;
    }
}
