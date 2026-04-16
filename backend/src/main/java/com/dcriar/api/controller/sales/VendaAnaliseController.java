package com.dcriar.api.controller.sales;

import com.dcriar.api.dto.response.sales.VendaAnalisePorCanalItemResponseDTO;
import com.dcriar.api.dto.response.sales.VendaAnalisePorProdutoItemResponseDTO;
import com.dcriar.api.hateoas.sales.assembler.VendaAnalisePorCanalModelAssembler;
import com.dcriar.api.hateoas.sales.assembler.VendaAnalisePorProdutoModelAssembler;
import com.dcriar.api.hateoas.sales.assembler.VendaAnaliseSerieTemporalModelAssembler;
import com.dcriar.api.hateoas.sales.assembler.VendaAnaliseTotaisModelAssembler;
import com.dcriar.api.hateoas.sales.model.VendaAnalisePorCanalModel;
import com.dcriar.api.hateoas.sales.model.VendaAnalisePorProdutoModel;
import com.dcriar.api.hateoas.sales.model.VendaAnaliseSerieTemporalModel;
import com.dcriar.api.hateoas.sales.model.VendaAnaliseTotaisModel;
import com.dcriar.domain.sales.service.VendaAnaliseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/vendas/analise")
@RequiredArgsConstructor
@Tag(name = "Vendas - Análise", description = "Endpoints de análise dinâmica de vendas por período")
public class VendaAnaliseController {

    private final VendaAnaliseService vendaAnaliseService;
    private final VendaAnaliseTotaisModelAssembler totaisAssembler;
    private final VendaAnalisePorCanalModelAssembler porCanalAssembler;
    private final VendaAnalisePorProdutoModelAssembler porProdutoAssembler;
    private final VendaAnaliseSerieTemporalModelAssembler serieTemporalAssembler;

    @GetMapping("/totais")
    @Operation(summary = "Consultar receita e total de pedidos do período, com comparação ao período anterior equivalente")
    public ResponseEntity<VendaAnaliseTotaisModel> consultarTotais(
            @Parameter(description = "Data inicial do período (inclusive).", example = "2026-04-09", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @Parameter(description = "Data final do período (inclusive).", example = "2026-04-16", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim
    ) {
        return totaisAssembler.toOkResponseEntity(vendaAnaliseService.consultarTotais(dataInicio, dataFim));
    }

    @GetMapping("/por-canal")
    @Operation(summary = "Consultar receita e pedidos agrupados por canal de venda no período, ordenados e paginados dinamicamente")
    public ResponseEntity<PagedModel<VendaAnalisePorCanalModel>> consultarPorCanal(
            @Parameter(description = "Data inicial do período (inclusive).", example = "2026-04-09", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @Parameter(description = "Data final do período (inclusive).", example = "2026-04-16", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @ParameterObject @PageableDefault(size = 10, sort = "receita", direction = Sort.Direction.DESC) Pageable pageable,
            PagedResourcesAssembler<VendaAnalisePorCanalItemResponseDTO> pagedResourcesAssembler
    ) {
        Page<VendaAnalisePorCanalItemResponseDTO> page = vendaAnaliseService.consultarPorCanal(dataInicio, dataFim, pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(page, porCanalAssembler));
    }

    @GetMapping("/por-produto")
    @Operation(summary = "Consultar receita e unidades vendidas agrupados por produto no período, ordenados e paginados dinamicamente")
    public ResponseEntity<PagedModel<VendaAnalisePorProdutoModel>> consultarPorProduto(
            @Parameter(description = "Data inicial do período (inclusive).", example = "2026-04-09", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @Parameter(description = "Data final do período (inclusive).", example = "2026-04-16", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @ParameterObject @PageableDefault(size = 5, sort = "receita", direction = Sort.Direction.DESC) Pageable pageable,
            PagedResourcesAssembler<VendaAnalisePorProdutoItemResponseDTO> pagedResourcesAssembler
    ) {
        Page<VendaAnalisePorProdutoItemResponseDTO> page = vendaAnaliseService.consultarPorProduto(dataInicio, dataFim, pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(page, porProdutoAssembler));
    }

    @GetMapping("/serie-temporal")
    @Operation(summary = "Consultar série temporal de receita e pedidos no período, com agrupamento automático por dia, semana ou mês")
    public ResponseEntity<VendaAnaliseSerieTemporalModel> consultarSerieTemporal(
            @Parameter(description = "Data inicial do período (inclusive).", example = "2026-04-09", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @Parameter(description = "Data final do período (inclusive).", example = "2026-04-16", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim
    ) {
        return serieTemporalAssembler.toOkResponseEntity(vendaAnaliseService.consultarSerieTemporal(dataInicio, dataFim));
    }
}
