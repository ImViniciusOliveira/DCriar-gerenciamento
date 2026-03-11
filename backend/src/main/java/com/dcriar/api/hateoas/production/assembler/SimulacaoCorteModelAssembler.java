package com.dcriar.api.hateoas.production.assembler;

import com.dcriar.api.dto.response.production.SimulacaoCorteResponseDTO;
import com.dcriar.api.hateoas.production.model.SimulacaoCorteModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Component
public class SimulacaoCorteModelAssembler extends RepresentationModelAssemblerSupport<SimulacaoCorteResponseDTO, SimulacaoCorteModel> {

    public SimulacaoCorteModelAssembler() {
        super(SimulacaoCorteModelAssembler.class, SimulacaoCorteModel.class);
    }

    @Override
    @NonNull
    public SimulacaoCorteModel toModel(@NonNull SimulacaoCorteResponseDTO dto) {
        SimulacaoCorteModel model = new SimulacaoCorteModel();
        model.setData(dto);

        // Adiciona o link para criar a ordem de corte manualmente
        String createOrderUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/ordens-de-producao/corte")
                .toUriString();
        model.add(Link.of(createOrderUrl, "create-order"));

        return model;
    }
}
