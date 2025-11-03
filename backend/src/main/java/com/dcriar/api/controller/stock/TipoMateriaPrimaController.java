package com.dcriar.api.controller.stock;

import com.dcriar.api.dto.request.stock.TipoMateriaPrimaRequestDTO;
import com.dcriar.api.dto.response.stock.TipoMateriaPrimaResponseDTO;
import com.dcriar.api.hateoas.stock.assembler.TipoMateriaPrimaModelAssembler;
import com.dcriar.api.hateoas.stock.model.TipoMateriaPrimaModel;
import com.dcriar.domain.stock.entity.enums.UnidadeDeMedida;
import com.dcriar.domain.stock.service.TipoMateriaPrimaService;
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
 * Controller REST para o gerenciamento de Tipos de Matéria-Prima.
 * Expõe os endpoints para as operações de CRUD, seguindo o padrão HATEOAS.
 */
@RestController
@RequestMapping("/api/v1/tipos-materia-prima")
@RequiredArgsConstructor
@Tag(name = "Tipos de Matéria-Prima", description = "Endpoints para o catálogo de insumos")
public class TipoMateriaPrimaController {

    private final TipoMateriaPrimaService tipoMateriaPrimaService;
    private final TipoMateriaPrimaModelAssembler tipoMateriaPrimaModelAssembler;

    @GetMapping
    @Operation(summary = "Listar todos os tipos de matéria-prima com filtros e paginação")
    @Parameters({
            @Parameter(name = "sort", description = "Critério de ordenação no formato: propriedade,asc|desc.", example = "nome,asc")
    })
    public ResponseEntity<PagedModel<TipoMateriaPrimaModel>> findAll(
            @Parameter(description = "Filtrar por parte do nome (case-insensitive)")
            @RequestParam(required = false) String nome,
            @Parameter(description = "Filtrar por unidade de consumo")
            @RequestParam(required = false) UnidadeDeMedida unidadeDeConsumo,
            @ParameterObject @PageableDefault(sort = "nome", direction = Sort.Direction.ASC) Pageable pageable,
            PagedResourcesAssembler<TipoMateriaPrimaResponseDTO> pagedResourcesAssembler) {

        Page<TipoMateriaPrimaResponseDTO> dtosPage = tipoMateriaPrimaService.findAll(nome, unidadeDeConsumo, pageable);

        PagedModel<TipoMateriaPrimaModel> pagedModel = pagedResourcesAssembler.toModel(dtosPage, tipoMateriaPrimaModelAssembler);

        return ResponseEntity.ok(pagedModel);
    }

    /**
     * Método de sobrecarga para a construção de links HATEOAS.
     * <p>
     * Não é um endpoint real e não deve ser chamado diretamente.
     * Sua única finalidade é servir como um alvo seguro para o {@code linkTo(methodOn(...))},
     * evitando a passagem de {@code null} para parâmetros que não devem ser nulos
     * e resolvendo a ambiguidade de qual método {@code findAll} chamar.
     * @return null, pois nunca é executado.
     */
    @SuppressWarnings("unused") // Usado por reflexão pelo Spring HATEOAS
    public PagedModel<TipoMateriaPrimaModel> findAll() {
        // O tipo de retorno corresponde ao que o assembler espera, mas o método nunca é executado.
        return null;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar tipo de matéria-prima por ID")
    public ResponseEntity<TipoMateriaPrimaModel> findById(@PathVariable Long id) {
        TipoMateriaPrimaResponseDTO dto = tipoMateriaPrimaService.findById(id);
        return tipoMateriaPrimaModelAssembler.toOkResponseEntity(dto);
    }

    @PostMapping
    @Operation(summary = "Criar um novo tipo de matéria-prima")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Tipo criado com sucesso.",
                    headers = @Header(name = "Location", description = "URL do novo recurso")),
            @ApiResponse(responseCode = "400", description = "Dados de requisição inválidos", content = @Content)
    })
    public ResponseEntity<TipoMateriaPrimaModel> create(@RequestBody @Valid TipoMateriaPrimaRequestDTO requestDTO) {
        TipoMateriaPrimaResponseDTO tipoCriado = tipoMateriaPrimaService.create(requestDTO);
        return tipoMateriaPrimaModelAssembler.toCreatedResponseEntity(tipoCriado);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Atualizar parcialmente um tipo de matéria-prima existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tipo atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados de requisição inválidos", content = @Content),
            @ApiResponse(responseCode = "404", description = "Tipo não encontrado", content = @Content)
    })
    public ResponseEntity<TipoMateriaPrimaModel> Update(
            @PathVariable Long id,
            @RequestBody TipoMateriaPrimaRequestDTO requestDTO) {
        TipoMateriaPrimaResponseDTO tipoAtualizado = tipoMateriaPrimaService.update(id, requestDTO);
        return tipoMateriaPrimaModelAssembler.toOkResponseEntity(tipoAtualizado);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deletar um tipo de matéria-prima")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Tipo deletado com sucesso."),
            @ApiResponse(responseCode = "404", description = "Tipo não encontrado", content = @Content)
    })
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        tipoMateriaPrimaService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
