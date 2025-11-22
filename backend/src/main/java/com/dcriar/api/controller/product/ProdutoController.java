package com.dcriar.api.controller.product;

import com.dcriar.api.dto.request.product.ProdutoRequestDTO;
import com.dcriar.api.dto.response.product.ProdutoDeCorteResponseDTO;
import com.dcriar.api.dto.response.product.ProdutoResponseDTO;
import com.dcriar.api.hateoas.product.assembler.ProdutoModelAssembler;
import com.dcriar.api.hateoas.product.model.ProdutoModel;
import com.dcriar.domain.product.service.ProdutoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
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
import org.springframework.hateoas.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Controller responsável por expor os endpoints da API para o recurso de Produto.
 */
@RestController
@RequestMapping("/api/v1/produtos")
@RequiredArgsConstructor
@Tag(name = "Produtos", description = "Endpoints para gerenciamento de produtos")
public class ProdutoController {

    private final ProdutoService produtoService;
    private final ProdutoModelAssembler produtoModelAssembler;

    @GetMapping
    @Operation(summary = "Listar todos os produtos de forma paginada")
    @ApiResponse(responseCode = "200", description = "Lista de produtos retornada com sucesso")
    public ResponseEntity<PagedModel<ProdutoModel>> findAll(
            @ParameterObject @PageableDefault(sort = "nome", direction = Sort.Direction.ASC) Pageable pageable,
            PagedResourcesAssembler<ProdutoResponseDTO> pagedResourcesAssembler
    ) {
        Page<ProdutoResponseDTO> produtosPage = produtoService.findAll(pageable);
        PagedModel<ProdutoModel> pagedModel = pagedResourcesAssembler.toModel(produtosPage, produtoModelAssembler);

        // Adiciona o link de descoberta para o endpoint otimizado de busca de estoques.
        // Esta é a forma mais segura e simples de gerar o link base.
        pagedModel.add(linkTo(methodOn(EstoqueProdutoController.class)
                .listarEstoquesPorListaDeProdutos(null)).withRel("estoques-por-produtos"));

        return ResponseEntity.ok(pagedModel);
    }

    /**
     * Método de sobrecarga para a construção de links HATEOAS.
     * Não é um endpoint real e não deve ser chamado diretamente.
     * Sua única finalidade é servir como um alvo seguro para o {@code linkTo(methodOn(...))},
     * evitando a passagem de {@code null} para parâmetros anotados como {@code @NonNull}.
     * @return null, pois nunca é executado.
     */
    @SuppressWarnings("unused") // Usado por reflexão pelo Spring HATEOAS
    public PagedModel<ProdutoModel> findAll() {
        return null;
    }

    /**
     * Retorna um modelo de produto "em branco" com os links HATEOAS necessários para a criação.
     * Este endpoint serve como um "template" para o frontend poder descobrir as URLs de ações relacionadas,
     * como a busca de tipos de matéria-prima, antes mesmo de um produto ser criado.
     *
     * @return Um modelo HATEOAS de um produto com valores padrão e links para ações.
     */
    @GetMapping("/new")
    @Operation(summary = "Obter um modelo de produto para criação")
    @ApiResponse(responseCode = "200", description = "Modelo de produto retornado com sucesso")
    public ResponseEntity<ProdutoModel> getNewProductTemplate() {
        // Retorna um DTO de uma subclasse concreta para que o assembler possa funcionar.
        return produtoModelAssembler.toOkResponseEntity(new ProdutoDeCorteResponseDTO());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar produto por ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Produto encontrado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado", content = @Content)
    })
    public ResponseEntity<ProdutoModel> findById(@PathVariable Long id) {
        ProdutoResponseDTO produto = produtoService.findById(id);
        return produtoModelAssembler.toOkResponseEntity(produto);
    }

    @PostMapping
    @Operation(summary = "Criar um novo produto")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Produto criado com sucesso",
                    headers = @Header(name = "Location", description = "URL do novo recurso")),
            @ApiResponse(responseCode = "400", description = "Dados de requisição inválidos", content = @Content)
    })
    public ResponseEntity<ProdutoModel> create(@RequestBody @Valid ProdutoRequestDTO requestDTO) {
        ProdutoResponseDTO produtoCriado = produtoService.create(requestDTO);
        return produtoModelAssembler.toCreatedResponseEntity(produtoCriado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar um produto por completo (PUT)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Produto atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados de requisição inválidos", content = @Content),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado", content = @Content)
    })
    public ResponseEntity<ProdutoModel> update(@PathVariable Long id, @RequestBody @Valid ProdutoRequestDTO requestDTO) {
        ProdutoResponseDTO produtoAtualizado = produtoService.update(id, requestDTO);
        return produtoModelAssembler.toOkResponseEntity(produtoAtualizado);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Atualizar parcialmente um produto (PATCH)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Produto atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados de requisição inválidos", content = @Content),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado", content = @Content)
    })
    public ResponseEntity<ProdutoModel> patch(@PathVariable Long id, @RequestBody Map<String, Object> fields) {
        ProdutoResponseDTO produtoAtualizado = produtoService.patch(id, fields);
        return produtoModelAssembler.toOkResponseEntity(produtoAtualizado);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deletar um produto")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Produto deletado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado", content = @Content)
    })
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        produtoService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(path = "/{id}/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Fazer upload da foto de um produto")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Foto atualizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Nenhum arquivo enviado ou arquivo inválido", content = @Content),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado", content = @Content)
    })
    public ResponseEntity<ProdutoModel> uploadFoto(@PathVariable Long id,
                                                   @RequestParam("file") MultipartFile file) {
        ProdutoResponseDTO produtoAtualizado = produtoService.uploadFoto(id, file);
        return produtoModelAssembler.toOkResponseEntity(produtoAtualizado);
    }
}
