package com.dcriar.api.hateoas.sales.assembler;

import com.dcriar.api.controller.sales.VendaAnaliseController;
import com.dcriar.api.dto.response.sales.VendaAnaliseSerieTemporalResponseDTO;
import com.dcriar.api.hateoas.sales.model.VendaAnaliseSerieTemporalModel;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

@Component
public class VendaAnaliseSerieTemporalModelAssembler extends RepresentationModelAssemblerSupport<VendaAnaliseSerieTemporalResponseDTO, VendaAnaliseSerieTemporalModel> {

    public VendaAnaliseSerieTemporalModelAssembler() {
        super(VendaAnaliseController.class, VendaAnaliseSerieTemporalModel.class);
    }

    @Override
    @NonNull
    public VendaAnaliseSerieTemporalModel toModel(@NonNull VendaAnaliseSerieTemporalResponseDTO dto) {
        VendaAnaliseSerieTemporalModel model = VendaAnaliseSerieTemporalModel.builder()
                .trendLabel(dto.getTrendLabel())
                .serie(dto.getSerie())
                .build();

        model.add(linkTo(VendaAnaliseController.class).withRel("analise-vendas"));

        return model;
    }

    public ResponseEntity<VendaAnaliseSerieTemporalModel> toOkResponseEntity(@NonNull VendaAnaliseSerieTemporalResponseDTO dto) {
        return ResponseEntity.ok(toModel(dto));
    }
}
