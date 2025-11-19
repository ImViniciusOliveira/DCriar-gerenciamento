package com.dcriar.api.controller.product;

import com.dcriar.api.dto.request.product.AjusteEstoqueProdutoRequestDTO;
import com.dcriar.api.dto.request.product.AjusteEstoqueRequestDTO;
import com.dcriar.api.dto.response.product.EstoqueResponseDTO;
import com.dcriar.api.dto.response.product.MovimentacaoProdutoResponseDTO;
import com.dcriar.api.dto.response.product.ProdutoEstoqueResponseDTO;
import com.dcriar.api.hateoas.product.assembler.EstoqueProdutoModelAssembler;
import com.dcriar.api.hateoas.product.assembler.MovimentacaoProdutoModelAssembler;
import com.dcriar.api.hateoas.product.model.EstoqueProdutoModel;
import com.dcriar.api.hateoas.product.model.MovimentacaoProdutoModel;
import com.dcriar.domain.product.service.EstoqueProdutoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Controller responsável por expor os endpoints da API para o recurso de Estoque de Produtos Acabados.
 * Implementa o padrão HATEOAS para enriquecer as respostas com links navegáveis.
 */
@RestController
@RequestMapping("/api/v1/estoques")
@RequiredArgsConstructor
@Tag(name = "Estoque - Produtos Acabados", description = "Endpoints para gerenciamento do estoque de produtos finalizados")
public class EstoqueProdutoController {

    private final EstoqueProdutoService estoqueProdutoService;
    private final EstoqueProdutoModelAssembler estoqueProdutoModelAssembler;
    private final MovimentacaoProdutoModelAssembler movimentacaoProdutoModelAssembler;

    /**
     * Ajusta o estoque de um produto em um canal de venda específico (distribuição).
     *
     * @param requestDTO DTO com os dados para o ajuste de estoque no canal.
     * @return Um ResponseEntity com o modelo HATEOAS do estoque atualizado.
     */
    @PostMapping("/ajustar-canal")
    @Operation(summary = "Ajustar o estoque de um produto em um canal de venda (distribuição)")
    public ResponseEntity<EstoqueProdutoModel> ajustarEstoqueCanal(@RequestBody @Valid AjusteEstoqueRequestDTO requestDTO) {
        EstoqueResponseDTO estoqueAtualizadoDTO = estoqueProdutoService.ajustarEstoque(requestDTO);
        return estoqueProdutoModelAssembler.toOkResponseEntity(estoqueAtualizadoDTO);
    }

    /**
     * Ajusta o estoque físico total de um produto, conhecido como "Estoque Mestre".
     * Esta operação afeta a quantidade total disponível do produto.
     *
     * @param requestDTO DTO com os dados para o ajuste do estoque físico.
     * @return Um ResponseEntity com status 204 No Content em caso de sucesso.
     */
    @PostMapping("/ajuste-fisico")
    @Operation(summary = "Ajustar o Estoque Físico Total de um produto (o \"Estoque Mestre\")")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Estoque físico ajustado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Dados de requisição inválidos.", content = @Content),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado.", content = @Content)
    })
    public ResponseEntity<Void> ajustarEstoqueFisico(@RequestBody @Valid AjusteEstoqueProdutoRequestDTO requestDTO) {
        estoqueProdutoService.ajustarEstoqueFisico(requestDTO);
        return ResponseEntity.noContent().build();
    }

    /**
     * Consulta o estoque de um produto em um canal de venda específico.
     *
     * @param produtoId    O ID do produto a ser consultado.
     * @param canalVendaId O ID do canal de venda.
     * @return Um ResponseEntity com o modelo HATEOAS do estoque encontrado.
     */
    @GetMapping
    @Operation(summary = "Consultar o estoque de um produto em um canal específico")
    public ResponseEntity<EstoqueProdutoModel> consultarEstoque(
            @RequestParam Long produtoId,
            @RequestParam Long canalVendaId) {
        EstoqueResponseDTO estoqueDTO = estoqueProdutoService.consultarEstoque(produtoId, canalVendaId);
        return estoqueProdutoModelAssembler.toOkResponseEntity(estoqueDTO);
    }

    /**
     * Lista o estoque de todos os produtos, agrupados por canal de venda.
     * Este endpoint é útil para o frontend obter uma visão geral do estoque.
     *
     * @return Um ResponseEntity com a lista de DTOs de estoque de produtos.
     */
    @GetMapping("/por-produto-canais")
    @Operation(summary = "Listar o estoque de todos os produtos, agrupados por canal de venda")
    public ResponseEntity<List<ProdutoEstoqueResponseDTO>> listarEstoqueDeTodosOsProdutosPorCanal() {
        return ResponseEntity.ok(estoqueProdutoService.listarEstoqueDeTodosOsProdutosPorCanal());
    }

    /**
     * Lista o estoque de um produto específico, agrupado por todos os seus canais de venda.
     *
     * @param produtoId O ID do produto a ser consultado.
     * @return Um ResponseEntity com o DTO de estoque do produto ou 404 Not Found se não existir.
     */
    @GetMapping("/por-produto/{produtoId}/canais")
    @Operation(summary = "Listar o estoque de um produto, agrupado por canal de venda")
    public ResponseEntity<ProdutoEstoqueResponseDTO> listarEstoquesPorProduto(@PathVariable Long produtoId) {
        // Reutiliza o serviço que busca todos os estoques e filtra pelo produtoId desejado.
        // Isso evita a criação de uma nova consulta no banco de dados para um caso de uso específico.
        return estoqueProdutoService.listarEstoqueDeTodosOsProdutosPorCanal().stream()
                .filter(p -> p.getProdutoId().equals(produtoId))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/por-lista-produtos")
    @Operation(summary = "Listar estoques de múltiplos produtos (Otimizado)")
    public ResponseEntity<CollectionModel<ProdutoEstoqueResponseDTO>> listarEstoquesPorListaDeProdutos(
            @RequestParam(required = false) List<Long> produtoIds) {

        List<ProdutoEstoqueResponseDTO> estoques = estoqueProdutoService.listarEstoquePorListaDeProdutos(produtoIds);

        // Clean Code: Encapsula a lista em um modelo de coleção HATEOAS padrão
        CollectionModel<ProdutoEstoqueResponseDTO> collectionModel = CollectionModel.of(estoques);

        // Adiciona o link "self" apontando para este próprio método, permitindo navegação
        collectionModel.add(linkTo(methodOn(EstoqueProdutoController.class)
                .listarEstoquesPorListaDeProdutos(produtoIds)).withSelfRel());

        return ResponseEntity.ok(collectionModel);
    }

    /**
     * Consulta o histórico de movimentações do estoque físico total de um produto.
     *
     * @param produtoId O ID do produto para o qual o histórico será consultado.
     * @return Um ResponseEntity com uma coleção de modelos HATEOAS das movimentações de estoque.
     */
    @GetMapping("/fisico/por-produto/{produtoId}")
    @Operation(summary = "Consultar o histórico do Estoque Físico Total de um produto")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Histórico retornado com sucesso."),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado.", content = @Content)
    })
    public ResponseEntity<CollectionModel<MovimentacaoProdutoModel>> listarMovimentacoesPorProduto(@PathVariable Long produtoId) {
        List<MovimentacaoProdutoResponseDTO> historicoDTO = estoqueProdutoService.listarMovimentacoesPorProduto(produtoId);
        return movimentacaoProdutoModelAssembler.toOkResponseEntity(historicoDTO, produtoId);
    }
}