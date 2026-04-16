package com.dcriar.api.hateoas.sales.assembler;

import com.dcriar.api.controller.sales.VendaAnaliseController;
import com.dcriar.api.dto.response.sales.VendaAnalisePorCanalItemResponseDTO;
import com.dcriar.api.hateoas.sales.model.VendaAnalisePorCanalModel;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

@Component
public class VendaAnalisePorCanalModelAssembler extends RepresentationModelAssemblerSupport<VendaAnalisePorCanalItemResponseDTO, VendaAnalisePorCanalModel> {

    public VendaAnalisePorCanalModelAssembler() {
        super(VendaAnaliseController.class, VendaAnalisePorCanalModel.class);
    }

    @Override
    @NonNull
    public VendaAnalisePorCanalModel toModel(@NonNull VendaAnalisePorCanalItemResponseDTO dto) {
        VendaAnalisePorCanalModel model = VendaAnalisePorCanalModel.builder()
                .canalVendaId(dto.getCanalVendaId())
                .nomeCanal(dto.getNomeCanal())
                .receita(dto.getReceita())
                .totalPedidos(dto.getTotalPedidos())
                .build();

        model.add(linkTo(VendaAnaliseController.class).withRel("analise-vendas"));

        return model;
    }
}
