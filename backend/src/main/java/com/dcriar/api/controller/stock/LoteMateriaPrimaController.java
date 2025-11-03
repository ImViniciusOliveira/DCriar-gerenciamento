package com.dcriar.api.controller.stock;

import com.dcriar.api.dto.request.stock.LoteMateriaPrimaRequestDTO;
import com.dcriar.api.dto.request.stock.MovimentacaoRequestDTO;
import com.dcriar.api.dto.response.stock.LoteMateriaPrimaResponseDTO;
import com.dcriar.api.dto.response.stock.MovimentacaoResponseDTO;
import com.dcriar.api.hateoas.stock.assembler.LoteMateriaPrimaModelAssembler;
import com.dcriar.api.hateoas.stock.assembler.MovimentacaoLoteModelAssembler;
import com.dcriar.api.hateoas.stock.model.LoteMateriaPrimaModel;
import com.dcriar.api.hateoas.stock.model.MovimentacaoLoteModel;
import com.dcriar.domain.stock.service.LoteMateriaPrimaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
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

/**
 * Controller responsável por expor os endpoints da API para o recurso de Lote de Matéria-Prima.
 */
@RestController
@RequestMapping("/api/v1/lotes-materia-prima")
@RequiredArgsConstructor
@Tag(name = "Estoque - Lotes", description = "Endpoints para gerenciamento de lotes físicos de matéria-prima")
public class LoteMateriaPrimaController {

    private final LoteMateriaPrimaService loteMateriaPrimaService;
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
    @Operation(summary = "Listar todos os lotes de matéria-prima")
    @ApiResponse(responseCode = "200", description = "Lista de lotes retornada com sucesso")
    public ResponseEntity<CollectionModel<LoteMateriaPrimaModel>> findAll(
            @RequestParam(required = false) Long tipoMateriaPrimaId,
            @RequestParam(required = false) Boolean apenasLotesPrincipais) {
        List<LoteMateriaPrimaResponseDTO> lotes = loteMateriaPrimaService.findAll(tipoMateriaPrimaId, apenasLotesPrincipais);
        return ResponseEntity.ok(loteMateriaPrimaModelAssembler.toCollectionModel(lotes));
    }

    /**
     * Método de sobrecarga para a construção de links HATEOAS.
     * <p>
     * Não é um endpoint real e não deve ser chamado diretamente.
     * Sua única finalidade é servir como um alvo seguro para o {@code linkTo(methodOn(...))},
     * evitando a ambiguidade de qual método {@code findAll} chamar.
     * @return null, pois nunca é executado.
     */
    @SuppressWarnings("unused") // Usado por reflexão pelo Spring HATEOAS
    public org.springframework.hateoas.CollectionModel<LoteMateriaPrimaModel> findAll() {
        // O tipo de retorno é genérico, pois este método nunca é executado.
        return null;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar um lote por ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lote encontrado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Lote não encontrado", content = @Content)
    })
    public ResponseEntity<LoteMateriaPrimaModel> findById(@PathVariable Long id) {
        LoteMateriaPrimaResponseDTO lote = loteMateriaPrimaService.findById(id);
        return loteMateriaPrimaModelAssembler.toOkResponseEntity(lote);
    }

    @PostMapping("/{loteId}/movimentacoes")
    @Operation(summary = "Registar uma nova movimentação em um lote")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Movimentação registada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou saldo insuficiente", content = @Content),
            @ApiResponse(responseCode = "404", description = "Lote não encontrado", content = @Content)
    })
    public ResponseEntity<MovimentacaoLoteModel> registrarMovimentacao(
            @PathVariable Long loteId,
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
    public ResponseEntity<CollectionModel<MovimentacaoLoteModel>> listarMovimentacoes(@PathVariable Long loteId) {
        List<MovimentacaoResponseDTO> movimentacoes = loteMateriaPrimaService.listarMovimentacoesPorLote(loteId);
        return ResponseEntity.ok(movimentacaoLoteModelAssembler.toCollectionModel(movimentacoes, loteId));
    }
}
