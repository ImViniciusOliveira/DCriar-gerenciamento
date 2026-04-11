package com.dcriar.api.controller.stock;

import com.dcriar.api.dto.request.stock.PoliticaSaldoRetalhoAnaliseFiltro;
import com.dcriar.api.dto.response.stock.AnaliseEstoqueMateriaPrimaResponseDTO;
import com.dcriar.api.hateoas.stock.assembler.AnaliseEstoqueMateriaPrimaModelAssembler;
import com.dcriar.api.hateoas.stock.model.AnaliseEstoqueMateriaPrimaModel;
import com.dcriar.domain.stock.entity.enums.UnidadeDeMedida;
import com.dcriar.domain.stock.service.EstoqueMateriaPrimaAnaliseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/estoques/materias-primas")
@RequiredArgsConstructor
@Tag(name = "Estoque - Matérias-Primas", description = "Endpoints para leitura consolidada do estoque de matérias-primas")
public class EstoqueMateriaPrimaController {

    private final EstoqueMateriaPrimaAnaliseService estoqueMateriaPrimaAnaliseService;
    private final AnaliseEstoqueMateriaPrimaModelAssembler analiseEstoqueMateriaPrimaModelAssembler;

    @GetMapping("/analise")
    @Operation(summary = "Consultar a análise consolidada de estoque por tipo de matéria-prima")
    @Parameters({
            @Parameter(name = "sort", description = "Critério de ordenação. Nesta primeira versão, use campos do tipo de matéria-prima, como nome ou unidadeDeConsumo.", example = "nome,asc")
    })
    public ResponseEntity<PagedModel<AnaliseEstoqueMateriaPrimaModel>> listarAnaliseEstoque(
            @Parameter(description = "Filtrar por um tipo específico de matéria-prima.", example = "10")
            @RequestParam(required = false) Long tipoMateriaPrimaId,
            @Parameter(description = "Filtrar por parte do nome da matéria-prima.", example = "adesivo")
            @RequestParam(required = false) String nome,
            @Parameter(description = "Filtrar pela unidade de consumo principal do tipo.", example = "METRO_QUADRADO")
            @RequestParam(required = false) UnidadeDeMedida unidadeDeConsumo,
            @Parameter(description = "Filtrar tipos compatíveis com CORTE ou CONSUMO.", example = "CORTE")
            @RequestParam(required = false) String tipoProduto,
            @Parameter(description = "Define como os retalhos entram no saldo considerado.", example = "SEM_RETALHOS")
            @RequestParam(required = false, defaultValue = "TODOS") PoliticaSaldoRetalhoAnaliseFiltro politicaSaldoRetalho,
            @ParameterObject @PageableDefault(sort = "nome", direction = Sort.Direction.ASC) Pageable pageable,
            PagedResourcesAssembler<AnaliseEstoqueMateriaPrimaResponseDTO> pagedResourcesAssembler
    ) {
        Page<AnaliseEstoqueMateriaPrimaResponseDTO> page = estoqueMateriaPrimaAnaliseService.listarAnalise(
                tipoMateriaPrimaId,
                nome,
                unidadeDeConsumo,
                tipoProduto,
                politicaSaldoRetalho,
                pageable
        );

        return ResponseEntity.ok(pagedResourcesAssembler.toModel(page, analiseEstoqueMateriaPrimaModelAssembler));
    }
}
