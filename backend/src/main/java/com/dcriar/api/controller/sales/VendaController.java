package com.dcriar.api.controller.sales;

import com.dcriar.api.dto.request.sales.VendaRequestDTO;
import com.dcriar.api.dto.response.sales.VendaResponseDTO;
import com.dcriar.api.hateoas.sales.assembler.VendaModelAssembler;
import com.dcriar.api.hateoas.sales.model.VendaModel;
import com.dcriar.domain.sales.service.VendaService;
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
     * Lista todas as vendas registradas no sistema.
     * <p>
     * Exemplo de uso: GET /api/v1/vendas
     *
     * @return Lista de vendas com links HATEOAS
     */
    @GetMapping
    @Operation(summary = "Listar todas as vendas")
    @ApiResponse(responseCode = "200", description = "Lista de vendas retornada com sucesso.")
    public ResponseEntity<CollectionModel<VendaModel>> findAll() {
        List<VendaResponseDTO> vendas = vendaService.findAll();
        return ResponseEntity.ok(vendaModelAssembler.toCollectionModel(vendas));
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
    public ResponseEntity<VendaModel> findById(@PathVariable Long id) {
        VendaResponseDTO venda = vendaService.findById(id);
        return ResponseEntity.ok(vendaModelAssembler.toModel(venda));
    }
}
