package com.dcriar.api.hateoas.production.assembler;

import com.dcriar.api.dto.response.production.SimulacaoConsumoDiretoResponseDTO;
import com.dcriar.api.hateoas.production.model.SimulacaoConsumoDiretoModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Component
public class SimulacaoConsumoDiretoModelAssembler extends RepresentationModelAssemblerSupport<SimulacaoConsumoDiretoResponseDTO, SimulacaoConsumoDiretoModel> {

    public SimulacaoConsumoDiretoModelAssembler() {
        super(SimulacaoConsumoDiretoModelAssembler.class, SimulacaoConsumoDiretoModel.class);
    }

    @Override
    @NonNull
    public SimulacaoConsumoDiretoModel toModel(@NonNull SimulacaoConsumoDiretoResponseDTO dto) {
        SimulacaoConsumoDiretoModel model = new SimulacaoConsumoDiretoModel();
        model.setData(dto);

        // Adiciona o link para criar a ordem de consumo direto manualmente
        String createOrderUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/ordens-de-producao/consumo-direto")
                .toUriString();
        model.add(Link.of(createOrderUrl, "create-order"));

        return model;
    }
}
