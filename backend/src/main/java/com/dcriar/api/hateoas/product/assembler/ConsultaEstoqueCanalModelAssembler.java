package com.dcriar.api.hateoas.product.assembler;

import com.dcriar.api.controller.product.EstoqueProdutoController;
import com.dcriar.api.controller.product.ProdutoController;
import com.dcriar.api.dto.response.product.ConsultaEstoqueCanalResponseDTO;
import com.dcriar.api.hateoas.product.model.ConsultaEstoqueCanalModel;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class ConsultaEstoqueCanalModelAssembler extends RepresentationModelAssemblerSupport<ConsultaEstoqueCanalResponseDTO, ConsultaEstoqueCanalModel> {

    public ConsultaEstoqueCanalModelAssembler() {
        super(EstoqueProdutoController.class, ConsultaEstoqueCanalModel.class);
    }

    @Override
    @NonNull
    public ConsultaEstoqueCanalModel toModel(@NonNull ConsultaEstoqueCanalResponseDTO dto) {
        ConsultaEstoqueCanalModel model = ConsultaEstoqueCanalModel.fromDto(dto);

        model.add(linkTo(methodOn(EstoqueProdutoController.class)
                .consultarEstoque(model.getProdutoId(), model.getCanalVendaId()))
                .withSelfRel());
        model.add(linkTo(methodOn(ProdutoController.class).findById(model.getProdutoId())).withRel("produto"));
        model.add(linkTo(methodOn(EstoqueProdutoController.class).listarEstoquesPorProduto(model.getProdutoId())).withRel("canais-do-produto"));
        model.add(linkTo(methodOn(EstoqueProdutoController.class).listarMovimentacoesPorProduto(model.getProdutoId())).withRel("historico-fisico"));

        return model;
    }

    public ResponseEntity<ConsultaEstoqueCanalModel> toOkResponseEntity(@NonNull ConsultaEstoqueCanalResponseDTO dto) {
        return ResponseEntity.ok(toModel(dto));
    }
}
