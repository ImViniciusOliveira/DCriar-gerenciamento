package com.dcriar.api.controller.product;

import com.dcriar.api.dto.response.product.AnaliseEstoqueProdutoResponseDTO;
import com.dcriar.api.hateoas.product.assembler.AnaliseEstoqueProdutoModelAssembler;
import com.dcriar.api.hateoas.product.model.AnaliseEstoqueProdutoModel;
import com.dcriar.domain.product.entity.enums.StatusAnaliseProduto;
import com.dcriar.domain.product.service.EstoqueProdutoAnaliseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/estoques/produtos")
@RequiredArgsConstructor
@Tag(name = "Estoque - Produtos Acabados", description = "Endpoints de leitura analítica do estoque de produtos")
public class EstoqueProdutoAnaliseController {

    private final EstoqueProdutoAnaliseService estoqueProdutoAnaliseService;
    private final AnaliseEstoqueProdutoModelAssembler analiseEstoqueProdutoModelAssembler;

    @GetMapping("/analise")
    @Operation(summary = "Consultar a análise consolidada de saldo por produto")
    @Parameters({
            @Parameter(name = "sort", description = "Critério de ordenação. Use campos como nome, sku, saldoAtual, saldoConsiderado, estoqueCritico, percentualRisco ou statusAnalise.", example = "percentualRisco,desc")
    })
    public ResponseEntity<PagedModel<AnaliseEstoqueProdutoModel>> listarAnaliseEstoque(
            @Parameter(description = "Filtrar por um produto específico.", example = "11")
            @RequestParam(required = false) Long produtoId,
            @Parameter(description = "Filtrar por parte do nome ou SKU do produto.", example = "cartão")
            @RequestParam(required = false) String nome,
            @Parameter(description = "Filtrar por tipo de produto.", example = "CONSUMO")
            @RequestParam(required = false) String tipoProduto,
            @Parameter(description = "Filtrar pela classificação da análise.", example = "CRITICO")
            @RequestParam(required = false) StatusAnaliseProduto statusAnalise,
            @ParameterObject @PageableDefault(sort = "nome", direction = Sort.Direction.ASC) Pageable pageable,
            PagedResourcesAssembler<AnaliseEstoqueProdutoResponseDTO> pagedResourcesAssembler
    ) {
        Page<AnaliseEstoqueProdutoResponseDTO> page = estoqueProdutoAnaliseService.listarAnalise(
                produtoId,
                nome,
                tipoProduto,
                statusAnalise,
                pageable
        );

        return ResponseEntity.ok(pagedResourcesAssembler.toModel(page, analiseEstoqueProdutoModelAssembler));
    }
}
