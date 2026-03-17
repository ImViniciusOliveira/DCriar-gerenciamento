package com.dcriar.api.controller.product;

import com.dcriar.api.dto.request.product.CanalVendaRequestDTO;
import com.dcriar.api.dto.response.product.CanalVendaResponseDTO;
import com.dcriar.api.hateoas.product.assembler.CanalVendaModelAssembler;
import com.dcriar.api.hateoas.enums.model.CanalVendaModel;
import com.dcriar.domain.product.service.CanalVendaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller para gerenciamento de Canais de Venda.
 */
@RestController
@RequestMapping("/api/v1/canais-venda")
@RequiredArgsConstructor
@Tag(name = "Produtos - Canais de Venda", description = "Endpoints para gerenciamento de canais de venda")
public class CanalVendaController {

    private final CanalVendaService canalVendaService;
    private final CanalVendaModelAssembler canalVendaModelAssembler;

    @PostMapping
    @Operation(summary = "Criar um novo canal de venda")
    @ApiResponse(responseCode = "201", description = "Canal de venda criado com sucesso.")
    public ResponseEntity<CanalVendaModel> create(@RequestBody @Valid CanalVendaRequestDTO requestDTO) {
        CanalVendaResponseDTO responseDTO = canalVendaService.create(requestDTO);
        return canalVendaModelAssembler.toCreatedResponseEntity(responseDTO);
    }

    @GetMapping
    @Operation(summary = "Listar todos os canais de venda")
    @ApiResponse(responseCode = "200", description = "Lista de canais de venda retornada com sucesso.")
    public ResponseEntity<CollectionModel<CanalVendaModel>> findAll() {
        List<CanalVendaResponseDTO> responseDTOs = canalVendaService.findAll();
        return ResponseEntity.ok(canalVendaModelAssembler.toCollectionModel(responseDTOs));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar um canal de venda por ID")
    @ApiResponse(responseCode = "200", description = "Canal de venda encontrado com sucesso.")
    @ApiResponse(responseCode = "404", description = "Canal de venda não encontrado.")
    public ResponseEntity<CanalVendaModel> findById(@Parameter(description = "ID do canal de venda.", example = "1") @PathVariable Long id) {
        CanalVendaResponseDTO responseDTO = canalVendaService.findById(id);
        return ResponseEntity.ok(canalVendaModelAssembler.toModel(responseDTO));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar um canal de venda")
    @ApiResponse(responseCode = "200", description = "Canal de venda atualizado com sucesso.")
    @ApiResponse(responseCode = "404", description = "Canal de venda não encontrado.")
    public ResponseEntity<CanalVendaModel> update(@Parameter(description = "ID do canal de venda a ser atualizado.", example = "1") @PathVariable Long id, @RequestBody @Valid CanalVendaRequestDTO requestDTO) {
        CanalVendaResponseDTO responseDTO = canalVendaService.update(id, requestDTO);
        return ResponseEntity.ok(canalVendaModelAssembler.toModel(responseDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir um canal de venda")
    @ApiResponse(responseCode = "204", description = "Canal de venda excluído com sucesso.")
    @ApiResponse(responseCode = "404", description = "Canal de venda não encontrado.")
    public ResponseEntity<Void> delete(@Parameter(description = "ID do canal de venda a ser excluído.", example = "1") @PathVariable Long id) {
        canalVendaService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
