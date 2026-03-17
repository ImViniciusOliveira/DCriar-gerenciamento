package com.dcriar.api.hateoas.production.assembler;

import com.dcriar.api.dto.response.production.SimulacaoConsumoResponseDTO;
import com.dcriar.api.hateoas.production.model.SimulacaoConsumoModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Component
public class SimulacaoConsumoModelAssembler extends RepresentationModelAssemblerSupport<SimulacaoConsumoResponseDTO, SimulacaoConsumoModel> {

    public SimulacaoConsumoModelAssembler() {
        super(SimulacaoConsumoModelAssembler.class, SimulacaoConsumoModel.class);
    }

    @Override
    @NonNull
    public SimulacaoConsumoModel toModel(@NonNull SimulacaoConsumoResponseDTO dto) {
        SimulacaoConsumoModel model = new SimulacaoConsumoModel();
        model.setData(dto);

        // Adiciona o link para criar a ordem de consumo manualmente
        String createOrderUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/ordens-de-producao/consumo")
                .toUriString();
        model.add(Link.of(createOrderUrl, "create-order"));

        return model;
    }
}
