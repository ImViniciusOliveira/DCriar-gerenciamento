package com.dcriar.api.hateoas.product.assembler;

import com.dcriar.api.controller.product.EstoqueProdutoController;
import com.dcriar.api.controller.product.ProdutoController;
import com.dcriar.api.dto.response.product.ConsultaEstoqueResponseDTO;
import com.dcriar.api.hateoas.product.model.ConsultaEstoqueModel;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class ConsultaEstoqueModelAssembler extends RepresentationModelAssemblerSupport<ConsultaEstoqueResponseDTO, ConsultaEstoqueModel> {

    public ConsultaEstoqueModelAssembler() {
        super(EstoqueProdutoController.class, ConsultaEstoqueModel.class);
    }

    @Override
    @NonNull
    public ConsultaEstoqueModel toModel(@NonNull ConsultaEstoqueResponseDTO dto) {
        ConsultaEstoqueModel model = ConsultaEstoqueModel.fromDto(dto);

        model.add(linkTo(methodOn(EstoqueProdutoController.class)
                .listarConsultasEstoque(
                        model.getProdutoId(),
                        null,
                        model.getCanalVendaId(),
                        false,
                        PageRequest.of(0, 10, Sort.by("produto.nome").ascending()),
                        null
                ))
                .withSelfRel());
        model.add(linkTo(methodOn(ProdutoController.class).findById(model.getProdutoId())).withRel("produto"));
        model.add(linkTo(methodOn(EstoqueProdutoController.class)
                .listarHistoricoPorProduto(model.getProdutoId(), "all", null, PageRequest.of(0, 10, Sort.by("data").descending()), null))
                .withRel("historico-do-produto"));

        return model;
    }

    public ResponseEntity<ConsultaEstoqueModel> toOkResponseEntity(@NonNull ConsultaEstoqueResponseDTO dto) {
        return ResponseEntity.ok(toModel(dto));
    }
}
