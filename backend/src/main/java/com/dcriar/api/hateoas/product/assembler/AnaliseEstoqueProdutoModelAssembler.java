package com.dcriar.api.hateoas.product.assembler;

import com.dcriar.api.controller.product.EstoqueProdutoAnaliseController;
import com.dcriar.api.controller.product.EstoqueProdutoController;
import com.dcriar.api.controller.product.ProdutoController;
import com.dcriar.api.dto.response.product.AnaliseEstoqueProdutoResponseDTO;
import com.dcriar.api.hateoas.product.model.AnaliseEstoqueProdutoModel;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class AnaliseEstoqueProdutoModelAssembler extends RepresentationModelAssemblerSupport<AnaliseEstoqueProdutoResponseDTO, AnaliseEstoqueProdutoModel> {

    public AnaliseEstoqueProdutoModelAssembler() {
        super(EstoqueProdutoAnaliseController.class, AnaliseEstoqueProdutoModel.class);
    }

    @Override
    @NonNull
    public AnaliseEstoqueProdutoModel toModel(@NonNull AnaliseEstoqueProdutoResponseDTO dto) {
        AnaliseEstoqueProdutoModel model = AnaliseEstoqueProdutoModel.builder()
                .produtoId(dto.getProdutoId())
                .nomeProduto(dto.getNomeProduto())
                .skuProduto(dto.getSkuProduto())
                .tipoProduto(dto.getTipoProduto())
                .estoqueFisicoTotal(dto.getEstoqueFisicoTotal())
                .saldoConsiderado(dto.getSaldoConsiderado())
                .estoqueDistribuidoTotal(dto.getEstoqueDistribuidoTotal())
                .estoqueDisponivelParaAlocar(dto.getEstoqueDisponivelParaAlocar())
                .estoqueCritico(dto.getEstoqueCritico())
                .percentualRisco(dto.getPercentualRisco())
                .statusAnalise(dto.getStatusAnalise())
                .build();

        model.add(linkTo(methodOn(EstoqueProdutoAnaliseController.class)
                .listarAnaliseEstoque(
                        dto.getProdutoId(),
                        null,
                        dto.getTipoProduto(),
                        dto.getStatusAnalise(),
                        PageRequest.of(0, 10, Sort.by("nome").ascending()),
                        null
                ))
                .withSelfRel());
        model.add(linkTo(methodOn(ProdutoController.class).findById(dto.getProdutoId())).withRel("produto"));
        model.add(linkTo(methodOn(EstoqueProdutoController.class)
                .listarHistoricoPorProduto(dto.getProdutoId(), "all", null, null, null))
                .withRel("historico-do-produto"));

        return model;
    }
}
