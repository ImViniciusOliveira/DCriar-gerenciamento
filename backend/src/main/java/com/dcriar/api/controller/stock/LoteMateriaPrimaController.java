package com.dcriar.api.controller.stock;

import com.dcriar.api.dto.request.stock.AplicarAjusteLoteRequestDTO;
import com.dcriar.api.dto.request.stock.CalcularAjusteLoteRequestDTO;
import com.dcriar.api.dto.request.stock.LoteMateriaPrimaRequestDTO;
import com.dcriar.api.dto.request.stock.MovimentacaoRequestDTO;
import com.dcriar.api.dto.response.stock.CalcularAjusteLoteResponseDTO;
import com.dcriar.api.dto.response.stock.LoteMateriaPrimaResponseDTO;
import com.dcriar.api.dto.response.stock.MovimentacaoResponseDTO;
import com.dcriar.api.hateoas.stock.assembler.LoteMateriaPrimaModelAssembler;
import com.dcriar.api.hateoas.stock.assembler.MovimentacaoLoteModelAssembler;
import com.dcriar.api.hateoas.stock.model.LoteMateriaPrimaModel;
import com.dcriar.api.hateoas.stock.model.MovimentacaoLoteModel;
import com.dcriar.domain.stock.service.AjusteLoteService;
import com.dcriar.domain.stock.service.LoteMateriaPrimaService;
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
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller responsável por expor os endpoints da API para o recurso de Lote de Matéria-Prima.
 */
@RestController
@RequestMapping("/api/v1/lotes-materia-prima")
@RequiredArgsConstructor
@Tag(name = "Estoque - Lotes", description = "Endpoints para gerenciamento de lotes físicos de matéria-prima")
public class LoteMateriaPrimaController {

    private final LoteMateriaPrimaService loteMateriaPrimaService;
    private final AjusteLoteService ajusteLoteService;
    private final LoteMateriaPrimaModelAssembler loteMateriaPrimaModelAssembler;
    private final MovimentacaoLoteModelAssembler movimentacaoLoteModelAssembler;

    @PostMapping
    @Operation(summary = "Dar entrada de um novo lote no estoque")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Lote criado com sucesso",
                    headers = @Header(name = "Location", description = "URL do novo recurso")),
            @ApiResponse(responseCode = "400", description = "Dados de requisição inválidos", content = @Content)
    })
    public ResponseEntity<LoteMateriaPrimaModel> create(@RequestBody @Valid LoteMateriaPrimaRequestDTO requestDTO) {
        LoteMateriaPrimaResponseDTO loteCriado = loteMateriaPrimaService.create(requestDTO);
        return loteMateriaPrimaModelAssembler.toCreatedResponseEntity(loteCriado);
    }

    @GetMapping
    @Operation(summary = "Listar e buscar lotes de matéria-prima com filtros e paginação")
    @ApiResponse(responseCode = "200", description = "Lista de lotes retornada com sucesso")
    @Parameters({
            @Parameter(name = "sort", description = "Critério de ordenação no formato: propriedade,asc|desc.", example = "id,asc")
    })
    public ResponseEntity<PagedModel<LoteMateriaPrimaModel>> searchAll(
            @Parameter(description = "Filtrar pelo ID do tipo de matéria-prima.", example = "6")
            @RequestParam(required = false) Long tipoMateriaPrimaId,
            @Parameter(description = "Filtrar apenas por lotes principais.", example = "true")
            @RequestParam(required = false) Boolean apenasLotesPrincipais,
            @ParameterObject @PageableDefault(sort = "tipoMateriaPrima.nome", direction = Sort.Direction.ASC) Pageable pageable,
            PagedResourcesAssembler<LoteMateriaPrimaResponseDTO> pagedResourcesAssembler) {
        Page<LoteMateriaPrimaResponseDTO> lotesPage = loteMateriaPrimaService.findAll(tipoMateriaPrimaId, apenasLotesPrincipais, pageable);
        PagedModel<LoteMateriaPrimaModel> pagedModel = pagedResourcesAssembler.toModel(lotesPage, loteMateriaPrimaModelAssembler);
        return ResponseEntity.ok(pagedModel);
    }

    @GetMapping("/new")
    @Operation(summary = "Obter um modelo 'esqueleto' para criação de um novo lote")
    @ApiResponse(responseCode = "200", description = "Modelo retornado com sucesso")
    public ResponseEntity<LoteMateriaPrimaModel> getNewTemplate() {
        return loteMateriaPrimaModelAssembler.toOkResponseEntity(new LoteMateriaPrimaResponseDTO());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar um lote por ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lote encontrado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Lote não encontrado", content = @Content)
    })
    public ResponseEntity<LoteMateriaPrimaModel> findById(@Parameter(description = "ID do lote.", example = "6") @PathVariable Long id) {
        LoteMateriaPrimaResponseDTO lote = loteMateriaPrimaService.findById(id);
        return loteMateriaPrimaModelAssembler.toOkResponseEntity(lote);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar um lote de matéria-prima existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lote atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados de requisição inválidos", content = @Content),
            @ApiResponse(responseCode = "404", description = "Lote não encontrado", content = @Content)
    })
    public ResponseEntity<LoteMateriaPrimaModel> update(@Parameter(description = "ID do lote a ser atualizado.", example = "6") @PathVariable Long id, @RequestBody @Valid LoteMateriaPrimaRequestDTO requestDTO) {
        LoteMateriaPrimaResponseDTO loteAtualizado = loteMateriaPrimaService.update(id, requestDTO);
        return loteMateriaPrimaModelAssembler.toOkResponseEntity(loteAtualizado);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir um lote de matéria-prima")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Lote excluído com sucesso"),
            @ApiResponse(responseCode = "404", description = "Lote não encontrado", content = @Content)
    })
    public ResponseEntity<Void> delete(@Parameter(description = "ID do lote a ser excluído.", example = "6") @PathVariable Long id) {
        loteMateriaPrimaService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{loteId}/movimentacoes")
    @Operation(summary = "Registar uma nova movimentação em um lote")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Movimentação registada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou saldo insuficiente", content = @Content),
            @ApiResponse(responseCode = "404", description = "Lote não encontrado", content = @Content)
    })
    public ResponseEntity<MovimentacaoLoteModel> registrarMovimentacao(
            @Parameter(description = "ID do lote que receberá a movimentação.", example = "6") @PathVariable Long loteId,
            @RequestBody @Valid MovimentacaoRequestDTO requestDTO) {
        MovimentacaoResponseDTO movimentacao = loteMateriaPrimaService.registrarMovimentacao(loteId, requestDTO);
        return movimentacaoLoteModelAssembler.toCreatedResponseEntity(movimentacao, loteId);
    }

    @GetMapping("/{loteId}/movimentacoes")
    @Operation(summary = "Consultar o histórico de movimentações de um lote")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Histórico retornado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Lote não encontrado", content = @Content)
    })
    public ResponseEntity<CollectionModel<MovimentacaoLoteModel>> listarMovimentacoes(@Parameter(description = "ID do lote.", example = "6") @PathVariable Long loteId) {
        List<MovimentacaoResponseDTO> movimentacoes = loteMateriaPrimaService.listarMovimentacoesPorLote(loteId);
        return ResponseEntity.ok(movimentacaoLoteModelAssembler.toCollectionModel(movimentacoes, loteId));
    }

    @PostMapping("/{loteId}/ajustes/calcular")
    @Operation(summary = "Calcular o impacto de um ajuste operacional em um lote")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Preview do ajuste calculado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou saldo insuficiente", content = @Content),
            @ApiResponse(responseCode = "404", description = "Lote não encontrado", content = @Content)
    })
    public ResponseEntity<CalcularAjusteLoteResponseDTO> calcularAjuste(
            @Parameter(description = "ID do lote ajustado.", example = "6") @PathVariable Long loteId,
            @RequestBody @Valid CalcularAjusteLoteRequestDTO requestDTO) {
        return ResponseEntity.ok(ajusteLoteService.calcular(loteId, requestDTO));
    }

    @PostMapping("/{loteId}/ajustes/aplicar")
    @Operation(summary = "Aplicar um ajuste operacional em um lote")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ajuste aplicado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou saldo insuficiente", content = @Content),
            @ApiResponse(responseCode = "404", description = "Lote não encontrado", content = @Content)
    })
    public ResponseEntity<LoteMateriaPrimaModel> aplicarAjuste(
            @Parameter(description = "ID do lote ajustado.", example = "6") @PathVariable Long loteId,
            @RequestBody @Valid AplicarAjusteLoteRequestDTO requestDTO) {
        LoteMateriaPrimaResponseDTO loteAtualizado = ajusteLoteService.aplicar(loteId, requestDTO);
        return loteMateriaPrimaModelAssembler.toOkResponseEntity(loteAtualizado);
    }
}
