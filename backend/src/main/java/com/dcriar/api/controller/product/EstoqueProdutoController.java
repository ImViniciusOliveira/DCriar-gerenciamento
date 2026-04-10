package com.dcriar.api.controller.product;

import com.dcriar.api.dto.request.product.AjusteEstoqueProdutoRequestDTO;
import com.dcriar.api.dto.request.product.AjusteEstoqueRequestDTO;
import com.dcriar.api.dto.response.product.AjusteEstoqueCanalResumoDTO;
import com.dcriar.api.dto.response.product.AjusteEstoqueProdutoResumoDTO;
import com.dcriar.api.dto.response.product.ConsultaEstoqueResponseDTO;
import com.dcriar.api.dto.response.product.EstoqueProdutoResumoDTO;
import com.dcriar.api.dto.response.product.EstoqueResponseDTO;
import com.dcriar.api.dto.response.product.HistoricoEstoqueConsolidadoResponseDTO;
import com.dcriar.api.dto.response.product.MovimentacaoProdutoResponseDTO;
import com.dcriar.api.dto.response.product.ProdutoEstoqueResponseDTO;
import com.dcriar.api.hateoas.product.assembler.ConsultaEstoqueModelAssembler;
import com.dcriar.api.hateoas.product.assembler.EstoqueProdutoModelAssembler;
import com.dcriar.api.hateoas.product.assembler.MovimentacaoProdutoModelAssembler;
import com.dcriar.api.hateoas.product.model.ConsultaEstoqueModel;
import com.dcriar.api.hateoas.product.model.EstoqueProdutoModel;
import com.dcriar.api.hateoas.product.model.MovimentacaoProdutoModel;
import com.dcriar.domain.product.entity.enums.TipoMovimentacaoProduto;
import com.dcriar.domain.product.service.EstoqueProdutoService;
import com.dcriar.api.controller.stock.LoteMateriaPrimaController;
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
    private final ConsultaEstoqueModelAssembler consultaEstoqueModelAssembler;
    private final EstoqueProdutoModelAssembler estoqueProdutoModelAssembler;
    private final MovimentacaoProdutoModelAssembler movimentacaoProdutoModelAssembler;

    @GetMapping
    @Operation(summary = "Ponto de entrada para recursos de Estoque", description = "Retorna links para as operações disponíveis de estoque.")
    public ResponseEntity<RepresentationModel<?>> getRoot() {
        RepresentationModel<?> rootModel = new RepresentationModel<>();
        
        rootModel.add(linkTo(methodOn(EstoqueProdutoController.class).getRoot()).withSelfRel());
        
        // Link para busca resumida (usado em autocompletes)
        rootModel.add(linkTo(EstoqueProdutoController.class)
                .slash("resumo")
                .withRel("resumo"));
                
        // Link para listagem por lista de produtos
        rootModel.add(linkTo(methodOn(EstoqueProdutoController.class)
                .listarEstoquesPorListaDeProdutos(null))
                .withRel("por-lista-produtos"));
                
        // Link para listagem completa agrupada por canais
        rootModel.add(linkTo(methodOn(EstoqueProdutoController.class)
                .listarEstoqueDeTodosOsProdutosPorCanal())
                .withRel("todos-por-canais"));

        rootModel.add(linkTo(methodOn(EstoqueProdutoController.class)
                .listarHistoricoConsolidado("all", null, null, null, null, null))
                .withRel("historico"));

        rootModel.add(linkTo(methodOn(EstoqueProdutoController.class)
                .consultarEstoque(null, null))
                .withRel("consulta"));

        rootModel.add(linkTo(methodOn(EstoqueProdutoController.class)
                .listarConsultasEstoque(null, null, null, false, null, null))
                .withRel("consultas"));

        rootModel.add(linkTo(methodOn(EstoqueProdutoController.class)
                .listarProdutosParaAjuste(null, null, null, null))
                .withRel("ajustes-produtos"));

        rootModel.add(linkTo(methodOn(EstoqueProdutoController.class)
                .listarCanaisParaAjuste(null, null, null, null))
                .withRel("ajustes-canais"));

        rootModel.add(linkTo(methodOn(LoteMateriaPrimaController.class)
                .searchAllForAdjustments(null, null, null, null, null))
                .withRel("ajustes-lotes"));

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
    public ResponseEntity<ConsultaEstoqueModel> consultarEstoque(
            @Parameter(description = "ID do produto.", example = "1") @RequestParam Long produtoId,
            @Parameter(description = "ID do canal de venda.", example = "1") @RequestParam Long canalVendaId) {
        ConsultaEstoqueResponseDTO consultaDTO = estoqueProdutoService.consultarEstoqueParaConsulta(produtoId, canalVendaId);
        return consultaEstoqueModelAssembler.toOkResponseEntity(consultaDTO);
    }

    @GetMapping("/consultas")
    @Operation(summary = "Consultar estoques de forma dinâmica")
    @Parameters({
            @Parameter(name = "produtoId", description = "ID do produto para filtrar a consulta.", example = "1"),
            @Parameter(name = "nomeProduto", description = "Parte do nome ou SKU do produto.", example = "adesivo"),
            @Parameter(name = "canalVendaId", description = "ID do canal de venda para filtrar a consulta.", example = "2"),
            @Parameter(name = "apenasComSaldo", description = "Quando true, retorna apenas vínculos com saldo positivo no canal.", example = "true"),
            @Parameter(name = "sort", description = "Critério de ordenação.", example = "nomeProduto,asc")
    })
    public ResponseEntity<PagedModel<ConsultaEstoqueModel>> listarConsultasEstoque(
            @RequestParam(required = false) Long produtoId,
            @RequestParam(required = false) String nomeProduto,
            @RequestParam(required = false) Long canalVendaId,
            @RequestParam(required = false, defaultValue = "false") boolean apenasComSaldo,
            @ParameterObject @PageableDefault(size = 10, sort = "produto.nome", direction = Sort.Direction.ASC) Pageable pageable,
            PagedResourcesAssembler<ConsultaEstoqueResponseDTO> pagedResourcesAssembler
    ) {
        Page<ConsultaEstoqueResponseDTO> page = estoqueProdutoService
                .listarConsultasEstoque(produtoId, nomeProduto, canalVendaId, apenasComSaldo, pageable);

        return ResponseEntity.ok(pagedResourcesAssembler.toModel(page, consultaEstoqueModelAssembler));
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

    @GetMapping("/ajustes/produtos")
    @Operation(summary = "Listar produtos prontos para ajuste físico")
    @Parameters({
            @Parameter(name = "sort", description = "Critério de ordenação.", example = "nome,asc")
    })
    public ResponseEntity<PagedModel<EntityModel<AjusteEstoqueProdutoResumoDTO>>> listarProdutosParaAjuste(
            @Parameter(description = "Filtrar por nome ou SKU do produto.", example = "Resina")
            @RequestParam(required = false) String nomeProduto,
            @Parameter(description = "Filtrar por tipo de produto.", example = "CONSUMO")
            @RequestParam(required = false) String tipoProduto,
            @ParameterObject @PageableDefault(sort = "nome", direction = Sort.Direction.ASC) Pageable pageable,
            PagedResourcesAssembler<AjusteEstoqueProdutoResumoDTO> pagedResourcesAssembler
    ) {
        Page<AjusteEstoqueProdutoResumoDTO> page = estoqueProdutoService.listarProdutosParaAjuste(nomeProduto, tipoProduto, pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(page));
    }

    @GetMapping("/ajustes/canais")
    @Operation(summary = "Listar distribuições por canal prontas para ajuste")
    @Parameters({
            @Parameter(name = "sort", description = "Critério de ordenação.", example = "produto.nome,asc")
    })
    public ResponseEntity<PagedModel<EntityModel<AjusteEstoqueCanalResumoDTO>>> listarCanaisParaAjuste(
            @Parameter(description = "Filtrar por nome ou SKU do produto.", example = "Resina")
            @RequestParam(required = false) String nomeProduto,
            @Parameter(description = "Filtrar por um canal de venda específico.", example = "1")
            @RequestParam(required = false) Long canalVendaId,
            @ParameterObject @PageableDefault(sort = "produto.nome", direction = Sort.Direction.ASC) Pageable pageable,
            PagedResourcesAssembler<AjusteEstoqueCanalResumoDTO> pagedResourcesAssembler
    ) {
        Page<AjusteEstoqueCanalResumoDTO> page = estoqueProdutoService.listarCanaisParaAjuste(nomeProduto, canalVendaId, pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(page));
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

    @GetMapping("/historico")
    @Operation(summary = "Consultar o histórico consolidado das movimentações de estoque")
    @Parameters({
            @Parameter(name = "periodo", description = "Filtro rápido por período.", example = "1m"),
            @Parameter(name = "sort", description = "Critério de ordenação.", example = "data,desc")
    })
    public ResponseEntity<PagedModel<EntityModel<HistoricoEstoqueConsolidadoResponseDTO>>> listarHistoricoConsolidado(
            @RequestParam(required = false, defaultValue = "all") String periodo,
            @RequestParam(required = false) Long produtoId,
            @RequestParam(required = false) String nomeProduto,
            @RequestParam(required = false) TipoMovimentacaoProduto tipoMovimentacao,
            @ParameterObject @PageableDefault(sort = "data", direction = Sort.Direction.DESC) Pageable pageable,
            PagedResourcesAssembler<HistoricoEstoqueConsolidadoResponseDTO> pagedResourcesAssembler
    ) {
        Page<HistoricoEstoqueConsolidadoResponseDTO> page = estoqueProdutoService
                .listarHistoricoConsolidado(periodo, produtoId, nomeProduto, tipoMovimentacao, pageable);

        return ResponseEntity.ok(pagedResourcesAssembler.toModel(page));
    }

}
