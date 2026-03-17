package com.dcriar.api.controller.production;

import com.dcriar.api.dto.request.production.*;
import com.dcriar.api.dto.response.production.OrdemDeConsumoResponseDTO;
import com.dcriar.api.dto.response.production.OrdemDeProducaoResponseDTO;
import com.dcriar.api.dto.response.production.SimulacaoConsumoResponseDTO;
import com.dcriar.api.dto.response.production.SimulacaoCorteResponseDTO;
import com.dcriar.api.hateoas.production.assembler.OrdemDeConsumoModelAssembler;
import com.dcriar.api.hateoas.production.assembler.OrdemDeProducaoModelAssembler;
import com.dcriar.api.hateoas.production.assembler.SimulacaoConsumoModelAssembler;
import com.dcriar.api.hateoas.production.assembler.SimulacaoCorteModelAssembler;
import com.dcriar.api.hateoas.production.model.OrdemDeConsumoModel;
import com.dcriar.api.hateoas.production.model.OrdemDeProducaoModel;
import com.dcriar.api.hateoas.production.model.SimulacaoConsumoModel;
import com.dcriar.api.hateoas.production.model.SimulacaoCorteModel;
import com.dcriar.domain.production.service.OrdemDeProducaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
 * Controller responsável por expor os endpoints relacionados a Ordens de Produção.
 */
@RestController
@RequestMapping("/api/v1/ordens-de-producao")
@RequiredArgsConstructor
@Tag(name = "Produção - Ordens de Produção", description = "Endpoints para gerenciamento de ordens de produção")
public class OrdemDeProducaoController {

    private final OrdemDeProducaoService ordemDeProducaoService;
    private final OrdemDeProducaoModelAssembler ordemDeProducaoModelAssembler;
    private final OrdemDeConsumoModelAssembler ordemDeConsumoModelAssembler;
    private final SimulacaoCorteModelAssembler simulacaoCorteModelAssembler;
    private final SimulacaoConsumoModelAssembler simulacaoConsumoModelAssembler;

    @GetMapping("/new")
    @Operation(summary = "Obter um modelo 'esqueleto' para criação de uma nova ordem de produção")
    @ApiResponse(responseCode = "200", description = "Modelo retornado com sucesso")
    public ResponseEntity<OrdemDeProducaoModel> getNewTemplate() {
        return ordemDeProducaoModelAssembler.toOkResponseEntity(new OrdemDeProducaoResponseDTO());
    }

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

    @PostMapping("/consumo")
    @Operation(summary = "Criar uma nova ordem de produção do tipo CONSUMO")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Ordem de produção criada com sucesso.",
                    headers = @Header(name = "Location", description = "URL do novo recurso")),
            @ApiResponse(responseCode = "400", description = "Dados de requisição inválidos.", content = @Content)
    })
    public ResponseEntity<OrdemDeConsumoModel> criarOrdemDeConsumo(@RequestBody @Valid OrdemDeConsumoRequestDTO requestDTO) {
        OrdemDeConsumoResponseDTO responseDTO = ordemDeProducaoService.criarOrdemDeConsumo(requestDTO);
        return ordemDeConsumoModelAssembler.toCreatedResponseEntity(responseDTO);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar ordem de produção por ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ordem de produção encontrada com sucesso."),
            @ApiResponse(responseCode = "404", description = "Ordem de produção não encontrada.", content = @Content)
    })
    public ResponseEntity<OrdemDeProducaoModel> buscarPorId(
            @Parameter(description = "ID da ordem de produção.", example = "1")
            @PathVariable Long id) {
        OrdemDeProducaoResponseDTO responseDTO = ordemDeProducaoService.buscarPorId(id);
        OrdemDeProducaoModel model = ordemDeProducaoModelAssembler.toModel(responseDTO);
        return ResponseEntity.ok(model);
    }

    @GetMapping
    @Operation(summary = "Listar ordens de produção de forma paginada")
    @ApiResponse(responseCode = "200", description = "Lista paginada de ordens de produção retornada com sucesso.")
    public ResponseEntity<PagedModel<OrdemDeProducaoModel>> listar(
            @ParameterObject @PageableDefault(sort = "dataCriacao", direction = Sort.Direction.DESC) Pageable pageable,
            PagedResourcesAssembler<OrdemDeProducaoResponseDTO> assembler) {
        Page<OrdemDeProducaoResponseDTO> page = ordemDeProducaoService.listarPaginado(pageable);
        PagedModel<OrdemDeProducaoModel> pagedModel = assembler.toModel(page, ordemDeProducaoModelAssembler);
        return ResponseEntity.ok(pagedModel);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir uma ordem de produção")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Ordem de produção excluída com sucesso."),
            @ApiResponse(responseCode = "404", description = "Ordem de produção não encontrada.", content = @Content)
    })
    public ResponseEntity<Void> excluir(
            @Parameter(description = "ID da ordem de produção a ser excluída.", example = "1")
            @PathVariable Long id) {
        ordemDeProducaoService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/simular/corte")
    @Operation(summary = "Simular uma produção baseada em corte")
    @ApiResponse(responseCode = "200", description = "Simulação realizada com sucesso.")
    public ResponseEntity<SimulacaoCorteModel> simularCorte(@RequestBody @Valid SimulacaoCorteRequestDTO requestDTO) {
        SimulacaoCorteResponseDTO response = ordemDeProducaoService.simularCorte(requestDTO);
        return ResponseEntity.ok(simulacaoCorteModelAssembler.toModel(response));
    }

    @PostMapping("/verificar-corte")
    @Operation(summary = "Verificar e validar um layout de corte editado manualmente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Verificação realizada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Dados de edição inválidos.", content = @Content)
    })
    public ResponseEntity<SimulacaoCorteModel> verificarCorte(@RequestBody @Valid VerificacaoCorteRequestDTO requestDTO) {
        SimulacaoCorteResponseDTO response = ordemDeProducaoService.verificarCorte(requestDTO);
        return ResponseEntity.ok(simulacaoCorteModelAssembler.toModel(response));
    }

    @PostMapping("/simular/consumo")
    @Operation(summary = "Simular uma produção baseada em consumo (líquidos, pós, etc.)")
    @ApiResponse(responseCode = "200", description = "Simulação realizada com sucesso.")
    public ResponseEntity<SimulacaoConsumoModel> simularConsumo(@RequestBody @Valid SimulacaoConsumoRequestDTO requestDTO) {
        SimulacaoConsumoResponseDTO response = ordemDeProducaoService.simularConsumo(requestDTO);
        return ResponseEntity.ok(simulacaoConsumoModelAssembler.toModel(response));
    }
}
