package com.dcriar.api.controller.sales;

import com.dcriar.api.dto.request.sales.VendaRequestDTO;
import com.dcriar.api.dto.response.sales.VendaResponseDTO;
import com.dcriar.api.hateoas.sales.assembler.VendaModelAssembler;
import com.dcriar.api.hateoas.sales.model.VendaModel;
import com.dcriar.domain.sales.service.VendaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller responsável por expor os endpoints da API para o recurso de Vendas.
 */
@RestController
@RequestMapping("/api/v1/vendas")
@RequiredArgsConstructor
@Tag(name = "Vendas", description = "Endpoints para o registo e consulta de vendas")
public class VendaController {

    private final VendaService vendaService;
    private final VendaModelAssembler vendaModelAssembler;

    /**
     * Registra uma nova venda e orquestra a baixa automática de estoque.
     * <p>
     * Exemplo de uso: POST /api/v1/vendas
     *
     * @param requestDTO Dados da venda
     * @return Venda registrada com links HATEOAS e header Location
     */
    @PostMapping
    @Operation(summary = "Registar uma nova venda")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Venda registada com sucesso.",
                    headers = @Header(name = "Location", description = "URL do novo recurso")),
            @ApiResponse(responseCode = "400", description = "Dados de requisição inválidos ou estoque insuficiente.", content = @Content),
            @ApiResponse(responseCode = "404", description = "Produto ou Canal de Venda não encontrado.", content = @Content)
    })
    public ResponseEntity<VendaModel> registrarVenda(@RequestBody @Valid VendaRequestDTO requestDTO) {
        VendaResponseDTO vendaRegistrada = vendaService.registrarVenda(requestDTO);
        return vendaModelAssembler.toCreatedResponseEntity(vendaRegistrada);
    }

    /**
     * Atualiza uma venda existente.
     * <p>
     * Exemplo de uso: PUT /api/v1/vendas/{id}
     *
     * @param id ID da venda
     * @param requestDTO Novos dados da venda
     * @return Venda atualizada
     */
    @PutMapping("/{id}")
    @Operation(summary = "Atualizar uma venda existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Venda atualizada com sucesso."),
            @ApiResponse(responseCode = "404", description = "Venda não encontrada.", content = @Content)
    })
    public ResponseEntity<VendaModel> atualizarVenda(
            @Parameter(description = "ID da venda a ser atualizada.", example = "1")
            @PathVariable Long id,
            @RequestBody @Valid VendaRequestDTO requestDTO) {
        VendaResponseDTO vendaAtualizada = vendaService.atualizarVenda(id, requestDTO);
        return ResponseEntity.ok(vendaModelAssembler.toModel(vendaAtualizada));
    }

    /**
     * Remove uma venda do sistema.
     * <p>
     * Exemplo de uso: DELETE /api/v1/vendas/{id}
     *
     * @param id ID da venda
     * @return No Content
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Remover uma venda")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Venda removida com sucesso."),
            @ApiResponse(responseCode = "404", description = "Venda não encontrada.", content = @Content)
    })
    public ResponseEntity<Void> deletarVenda(
            @Parameter(description = "ID da venda a ser removida.", example = "1")
            @PathVariable Long id) {
        vendaService.deletarVenda(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Lista todas as vendas registradas no sistema de forma paginada.
     * <p>
     * Exemplo de uso: GET /api/v1/vendas?page=0&size=10&sort=id,asc
     *
     * @return Lista de vendas com links HATEOAS e informações de paginação.
     */
    @GetMapping
    @Operation(summary = "Listar todas as vendas")
    @ApiResponse(responseCode = "200", description = "Lista de vendas retornada com sucesso.")
    @Parameters({
            @Parameter(name = "sort", description = "Critério de ordenação no formato: propriedade,asc|desc.", example = "id,asc")
    })
    public ResponseEntity<PagedModel<VendaModel>> findAll(
            @ParameterObject @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
            PagedResourcesAssembler<VendaResponseDTO> pagedResourcesAssembler) {
        Page<VendaResponseDTO> vendasPage = vendaService.findAll(pageable);
        PagedModel<VendaModel> pagedModel = pagedResourcesAssembler.toModel(vendasPage, vendaModelAssembler);
        return ResponseEntity.ok(pagedModel);
    }

    /**
     * Retorna um modelo de venda "em branco" com os links HATEOAS necessários para a criação.
     * Este endpoint serve como um "template" para o frontend poder descobrir as URLs de ações relacionadas.
     *
     * @return Um modelo HATEOAS de uma venda com valores padrão e links para ações.
     */
    @GetMapping("/new")
    @Operation(summary = "Obter um modelo de venda para criação")
    @ApiResponse(responseCode = "200", description = "Modelo de venda retornado com sucesso")
    public ResponseEntity<VendaModel> getNewTemplate() {
        return vendaModelAssembler.toOkResponseEntity(new VendaResponseDTO());
    }

    /**
     * Busca os detalhes de uma venda específica pelo seu ID.
     * <p>
     * Exemplo de uso: GET /api/v1/vendas/{id}
     *
     * @param id ID da venda
     * @return Venda encontrada com links HATEOAS
     */
    @GetMapping("/{id}")
    @Operation(summary = "Buscar uma venda por ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Venda encontrada com sucesso."),
            @ApiResponse(responseCode = "404", description = "Venda não encontrada.", content = @Content)
    })
    public ResponseEntity<VendaModel> findById(
            @Parameter(description = "ID da venda.", example = "1")
            @PathVariable Long id) {
        VendaResponseDTO venda = vendaService.findById(id);
        return ResponseEntity.ok(vendaModelAssembler.toModel(venda));
    }
}
