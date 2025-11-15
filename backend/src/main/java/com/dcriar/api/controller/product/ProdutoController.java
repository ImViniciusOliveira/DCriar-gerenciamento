package com.dcriar.api.controller.product;

import com.dcriar.api.dto.request.product.ProdutoRequestDTO;
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

    /**
     * Lista todos os produtos cadastrados de forma paginada.
     *
     * @param pageable Parâmetros de paginação e ordenação.
     * @return Modelo paginado HATEOAS com a lista de produtos.
     */
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
        // Retorna um DTO vazio que o assembler transformará em um modelo com os links HATEOAS corretos.
        return produtoModelAssembler.toOkResponseEntity(new ProdutoResponseDTO());
    }

    /**
     * Busca um produto pelo seu ID.
     *
     * @param id ID do produto.
     * @return Produto encontrado com links HATEOAS.
     */
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

    /**
     * Cria um novo produto.
     *
     * @param requestDTO Dados do produto a ser criado.
     * @return Produto criado com links HATEOAS e header Location.
     */
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

    /**
     * Atualiza um produto existente por completo (PUT).
     *
     * @param id O ID do produto a ser atualizado.
     * @param requestDTO O DTO com os dados completos do produto.
     * @return O modelo HATEOAS do produto atualizado.
     */
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

    /**
     * Atualiza parcialmente um produto existente (PATCH).
     *
     * @param id O ID do produto a ser atualizado.
     * @param fields Um mapa com os campos a serem alterados.
     * @return O modelo HATEOAS do produto atualizado.
     */
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

    /**
     * Deleta um produto pelo seu ID.
     *
     * @param id ID do produto.
     * @return Resposta sem conteúdo (204) se deletado com sucesso.
     */
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

    /**
     * Realiza o upload de uma foto e a associa a um produto existente.
     * <p>
     * Este endpoint orquestra a ação de negócio de enviar um arquivo de imagem e
     * vinculá-lo a um produto específico em uma única operação atômica.
     *
     * @param id   O ID do produto ao qual a foto será associada.
     * @param file O arquivo de imagem enviado como 'multipart/form-data'.
     * @return Um ResponseEntity com status 200 OK e o modelo HATEOAS do produto atualizado.
     */
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