package com.dcriar.api.hateoas.sales.assembler;

import com.dcriar.api.controller.sales.VendaAnaliseController;
import com.dcriar.api.dto.response.sales.VendaAnaliseTotaisResponseDTO;
import com.dcriar.api.hateoas.sales.model.VendaAnaliseTotaisModel;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

@Component
public class VendaAnaliseTotaisModelAssembler extends RepresentationModelAssemblerSupport<VendaAnaliseTotaisResponseDTO, VendaAnaliseTotaisModel> {

    public VendaAnaliseTotaisModelAssembler() {
        super(VendaAnaliseController.class, VendaAnaliseTotaisModel.class);
    }

    @Override
    @NonNull
    public VendaAnaliseTotaisModel toModel(@NonNull VendaAnaliseTotaisResponseDTO dto) {
        VendaAnaliseTotaisModel model = VendaAnaliseTotaisModel.builder()
                .receita(dto.getReceita())
                .totalPedidos(dto.getTotalPedidos())
                .receitaPeriodoAnterior(dto.getReceitaPeriodoAnterior())
                .deltaPercent(dto.getDeltaPercent())
                .build();

        model.add(linkTo(VendaAnaliseController.class).withRel("analise-vendas"));

        return model;
    }

    public ResponseEntity<VendaAnaliseTotaisModel> toOkResponseEntity(@NonNull VendaAnaliseTotaisResponseDTO dto) {
        return ResponseEntity.ok(toModel(dto));
    }
}
