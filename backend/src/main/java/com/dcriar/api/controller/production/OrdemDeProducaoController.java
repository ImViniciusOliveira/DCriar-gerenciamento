package com.dcriar.api.controller.production;

import com.dcriar.api.dto.request.production.OrdemDeConsumoDiretoRequestDTO;
import com.dcriar.api.dto.request.production.OrdemDeCorteRequestDTO;
import com.dcriar.api.dto.request.production.SimulacaoConsumoDiretoRequestDTO;
import com.dcriar.api.dto.request.production.SimulacaoCorteRequestDTO;
import com.dcriar.api.dto.response.production.OrdemDeProducaoResponseDTO;
import com.dcriar.api.dto.response.production.SimulacaoConsumoDiretoResponseDTO;
import com.dcriar.api.dto.response.production.SimulacaoCorteResponseDTO;
import com.dcriar.api.hateoas.production.assembler.OrdemDeProducaoModelAssembler;
import com.dcriar.api.hateoas.production.model.OrdemDeProducaoModel;
import com.dcriar.domain.production.service.OrdemDeProducaoService;
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
 * Controller responsável por expor os endpoints relacionados a Ordens de Produção.
 */
@RestController
@RequestMapping("/api/v1/ordens-de-producao")
@RequiredArgsConstructor
@Tag(name = "Produção - Ordens de Produção", description = "Endpoints para gerenciamento de ordens de produção")
public class OrdemDeProducaoController {

    private final OrdemDeProducaoService ordemDeProducaoService;
    private final OrdemDeProducaoModelAssembler ordemDeProducaoModelAssembler;

    @PostMapping("/corte")
    @Operation(summary = "Criar uma nova ordem de produção do tipo CORTE")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Ordem de produção criada com sucesso.",
                    headers = @Header(name = "Location", description = "URL do novo recurso")),
            @ApiResponse(responseCode = "400", description = "Dados de requisição inválidos.", content = @Content)
    })
    public ResponseEntity<OrdemDeProducaoModel> criarOrdemDeCorte(@RequestBody @Valid OrdemDeCorteRequestDTO requestDTO) {
        OrdemDeProducaoResponseDTO responseDTO = ordemDeProducaoService.criarOrdemDeCorte(requestDTO);
        return ordemDeProducaoModelAssembler.toCreatedResponseEntity(responseDTO);
    }

    @PostMapping("/consumo-direto")
    @Operation(summary = "Criar uma nova ordem de produção do tipo CONSUMO DIRETO")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Ordem de produção criada com sucesso.",
                    headers = @Header(name = "Location", description = "URL do novo recurso")),
            @ApiResponse(responseCode = "400", description = "Dados de requisição inválidos.", content = @Content)
    })
    public ResponseEntity<OrdemDeProducaoModel> criarOrdemDeConsumoDireto(@RequestBody @Valid OrdemDeConsumoDiretoRequestDTO requestDTO) {
        OrdemDeProducaoResponseDTO responseDTO = ordemDeProducaoService.criarOrdemDeConsumoDireto(requestDTO);
        return ordemDeProducaoModelAssembler.toCreatedResponseEntity(responseDTO);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar ordem de produção por ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ordem de produção encontrada com sucesso."),
            @ApiResponse(responseCode = "404", description = "Ordem de produção não encontrada.", content = @Content)
    })
    public ResponseEntity<OrdemDeProducaoModel> buscarPorId(@PathVariable Long id) {
        OrdemDeProducaoResponseDTO responseDTO = ordemDeProducaoService.buscarPorId(id);
        OrdemDeProducaoModel model = ordemDeProducaoModelAssembler.toModel(responseDTO);
        return ResponseEntity.ok(model);
    }

    @GetMapping
    @Operation(summary = "Listar todas as ordens de produção")
    @ApiResponse(responseCode = "200", description = "Lista de ordens de produção retornada com sucesso.")
    public ResponseEntity<CollectionModel<OrdemDeProducaoModel>> listarTodas() {
        List<OrdemDeProducaoResponseDTO> dtos = ordemDeProducaoService.listarTodas();
        CollectionModel<OrdemDeProducaoModel> collectionModel = ordemDeProducaoModelAssembler.toCollectionModel(dtos);
        return ResponseEntity.ok(collectionModel);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir uma ordem de produção")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Ordem de produção excluída com sucesso."),
            @ApiResponse(responseCode = "404", description = "Ordem de produção não encontrada.", content = @Content)
    })
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        ordemDeProducaoService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/simular/corte")
    @Operation(summary = "Simular uma produção baseada em corte")
    @ApiResponse(responseCode = "200", description = "Simulação realizada com sucesso.")
    public ResponseEntity<SimulacaoCorteResponseDTO> simularCorte(@RequestBody @Valid SimulacaoCorteRequestDTO requestDTO) {
        SimulacaoCorteResponseDTO response = ordemDeProducaoService.simularCorte(requestDTO);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/simular/consumo-direto")
    @Operation(summary = "Simular uma produção baseada em consumo direto (líquidos, pós, etc.)")
    @ApiResponse(responseCode = "200", description = "Simulação realizada com sucesso.")
    public ResponseEntity<SimulacaoConsumoDiretoResponseDTO> simularConsumoDireto(@RequestBody @Valid SimulacaoConsumoDiretoRequestDTO requestDTO) {
        SimulacaoConsumoDiretoResponseDTO response = ordemDeProducaoService.simularConsumoDireto(requestDTO);
        return ResponseEntity.ok(response);
    }
}
