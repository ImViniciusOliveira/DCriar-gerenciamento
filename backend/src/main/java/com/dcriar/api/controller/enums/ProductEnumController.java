package com.dcriar.api.controller.enums;

import com.dcriar.api.hateoas.enums.assembler.TipoMovimentacaoProdutoModelAssembler;
import com.dcriar.api.hateoas.enums.model.TipoMovimentacaoProdutoModel;
import com.dcriar.domain.product.entity.enums.TipoMovimentacaoProduto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

/**
 * Controlador para expor enums e listas estáticas relacionados ao domínio de Produto.
 * <p>
 * Após a simplificação do modelo comercial, este controller permanece responsável
 * pelos tipos de movimentação de estoque de produto consumidos pelo frontend.
 */
@RestController
@RequestMapping("/api/v1/enums/product")
@Tag(name = "Enums - Produto", description = "Endpoints para consulta de enums relacionados a produtos")
public class ProductEnumController {

    private final TipoMovimentacaoProdutoModelAssembler tipoMovimentacaoProdutoModelAssembler;

    public ProductEnumController(TipoMovimentacaoProdutoModelAssembler tipoMovimentacaoProdutoModelAssembler) {
        this.tipoMovimentacaoProdutoModelAssembler = tipoMovimentacaoProdutoModelAssembler;
    }

    @GetMapping("/tipos-movimentacao-produto")
    @Operation(summary = "Listar todos os tipos de movimentação de produto")
    @ApiResponse(responseCode = "200", description = "Lista de tipos de movimentação de produto retornada com sucesso")
    public ResponseEntity<CollectionModel<TipoMovimentacaoProdutoModel>> getTiposMovimentacaoProduto() {
        return ResponseEntity.ok(tipoMovimentacaoProdutoModelAssembler.toCollectionModel(Arrays.asList(TipoMovimentacaoProduto.values())));
    }
}
