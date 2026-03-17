package com.dcriar.api.controller.product;

import com.dcriar.api.dto.request.product.AjusteEstoqueProdutoRequestDTO;
import com.dcriar.api.dto.request.product.AjusteEstoqueRequestDTO;
import com.dcriar.api.dto.response.product.EstoqueProdutoResumoDTO;
import com.dcriar.api.dto.response.product.EstoqueResponseDTO;
import com.dcriar.api.dto.response.product.MovimentacaoProdutoResponseDTO;
import com.dcriar.api.dto.response.product.ProdutoEstoqueResponseDTO;
import com.dcriar.api.hateoas.product.assembler.EstoqueProdutoModelAssembler;
import com.dcriar.api.hateoas.product.assembler.MovimentacaoProdutoModelAssembler;
import com.dcriar.api.hateoas.product.model.EstoqueProdutoModel;
import com.dcriar.api.hateoas.product.model.MovimentacaoProdutoModel;
import com.dcriar.domain.product.service.EstoqueProdutoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.RepresentationModel;
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

    @GetMapping
    @Operation(summary = "Ponto de entrada para recursos de Estoque", description = "Retorna links para as operações disponíveis de estoque.")
    public ResponseEntity<RepresentationModel<?>> getRoot() {
        RepresentationModel<?> rootModel = new RepresentationModel<>();
        
        rootModel.add(linkTo(methodOn(EstoqueProdutoController.class).getRoot()).withSelfRel());
        
        // Link para busca resumida (usado em autocompletes)
        rootModel.add(linkTo(methodOn(EstoqueProdutoController.class)
                .buscarEstoqueResumido(null, null, true, null, null))
                .withRel("resumo"));
                
        // Link para listagem por lista de produtos
        rootModel.add(linkTo(methodOn(EstoqueProdutoController.class)
                .listarEstoquesPorListaDeProdutos(null))
                .withRel("por-lista-produtos"));
                
        // Link para listagem completa agrupada por canais
        rootModel.add(linkTo(methodOn(EstoqueProdutoController.class)
                .listarEstoqueDeTodosOsProdutosPorCanal())
                .withRel("todos-por-canais"));

        return ResponseEntity.ok(rootModel);
    }

    @PostMapping("/ajustar-canal")
    @Operation(summary = "Ajustar o estoque de um produto em um canal de venda (distribuição)")
    public ResponseEntity<EstoqueProdutoModel> ajustarEstoqueCanal(@RequestBody @Valid AjusteEstoqueRequestDTO requestDTO) {
        EstoqueResponseDTO estoqueAtualizadoDTO = estoqueProdutoService.ajustarEstoque(requestDTO);
        return estoqueProdutoModelAssembler.toOkResponseEntity(estoqueAtualizadoDTO);
    }

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

    @GetMapping("/consulta") // Alterado de @GetMapping raiz para evitar conflito com getRoot
    @Operation(summary = "Consultar o estoque de um produto em um canal específico")
    public ResponseEntity<EstoqueProdutoModel> consultarEstoque(
            @Parameter(description = "ID do produto.", example = "1") @RequestParam Long produtoId,
            @Parameter(description = "ID do canal de venda.", example = "1") @RequestParam Long canalVendaId) {
        EstoqueResponseDTO estoqueDTO = estoqueProdutoService.consultarEstoque(produtoId, canalVendaId);
        return estoqueProdutoModelAssembler.toOkResponseEntity(estoqueDTO);
    }

    @GetMapping("/resumo")
    @Operation(summary = "Buscar resumo de estoque filtrado por canal e produto")
    @Parameters({
            @Parameter(name = "sort", description = "Critério de ordenação.", example = "produto.nome,asc")
    })
    public ResponseEntity<PagedModel<EntityModel<EstoqueProdutoResumoDTO>>> buscarEstoqueResumido(
            @Parameter(description = "ID do canal de venda.", example = "1") @RequestParam Long canalId,
            @Parameter(description = "Filtrar por parte do nome do produto.", example = "Cartão") @RequestParam(required = false) String nomeProduto,
            @Parameter(description = "Quando true, retorna apenas produtos com saldo no canal.", example = "true") @RequestParam(defaultValue = "true") boolean apenasComSaldo,
            @ParameterObject @PageableDefault(sort = "produto.nome", direction = Sort.Direction.ASC) Pageable pageable,
            PagedResourcesAssembler<EstoqueProdutoResumoDTO> pagedResourcesAssembler) {

        Page<EstoqueProdutoResumoDTO> page = estoqueProdutoService.buscarEstoqueResumido(canalId, nomeProduto, apenasComSaldo, pageable);
        
        // O PagedResourcesAssembler padrão converte Page<T> em PagedModel<EntityModel<T>>
        PagedModel<EntityModel<EstoqueProdutoResumoDTO>> pagedModel = pagedResourcesAssembler.toModel(page);
        
        return ResponseEntity.ok(pagedModel);
    }

    @GetMapping("/por-produto-canais")
    @Operation(summary = "Listar o estoque de todos os produtos, agrupados por canal de venda")
    public ResponseEntity<List<ProdutoEstoqueResponseDTO>> listarEstoqueDeTodosOsProdutosPorCanal() {
        return ResponseEntity.ok(estoqueProdutoService.listarEstoqueDeTodosOsProdutosPorCanal());
    }

    @GetMapping("/por-produto/{produtoId}/canais")
    @Operation(summary = "Listar o estoque de um produto, agrupado por canal de venda")
    public ResponseEntity<ProdutoEstoqueResponseDTO> listarEstoquesPorProduto(@Parameter(description = "ID do produto.", example = "1") @PathVariable Long produtoId) {
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
            @Parameter(description = "Lista de IDs de produtos para consulta em lote.", example = "1,3,11") @RequestParam(required = false) List<Long> produtoIds) {

        List<ProdutoEstoqueResponseDTO> estoques = estoqueProdutoService.listarEstoquePorListaDeProdutos(produtoIds);

        // Clean Code: Encapsula a lista em um modelo de coleção HATEOAS padrão
        CollectionModel<ProdutoEstoqueResponseDTO> collectionModel = CollectionModel.of(estoques);

        // Adiciona o link "self" apontando para este próprio método, permitindo navegação
        collectionModel.add(linkTo(methodOn(EstoqueProdutoController.class)
                .listarEstoquesPorListaDeProdutos(produtoIds)).withSelfRel());

        return ResponseEntity.ok(collectionModel);
    }

    @GetMapping("/fisico/por-produto/{produtoId}")
    @Operation(summary = "Consultar o histórico do Estoque Físico Total de um produto")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Histórico retornado com sucesso."),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado.", content = @Content)
    })
    public ResponseEntity<CollectionModel<MovimentacaoProdutoModel>> listarMovimentacoesPorProduto(@Parameter(description = "ID do produto.", example = "1") @PathVariable Long produtoId) {
        List<MovimentacaoProdutoResponseDTO> historicoDTO = estoqueProdutoService.listarMovimentacoesPorProduto(produtoId);
        return movimentacaoProdutoModelAssembler.toOkResponseEntity(historicoDTO, produtoId);
    }
}
