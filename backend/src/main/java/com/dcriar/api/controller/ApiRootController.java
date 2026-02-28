package com.dcriar.api.controller;

import com.dcriar.api.controller.product.CanalVendaController;
import com.dcriar.api.controller.product.ProdutoController;
import com.dcriar.api.controller.production.OrdemDeProducaoController;
import com.dcriar.api.controller.sales.VendaController;
import com.dcriar.api.controller.stock.LoteMateriaPrimaController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

/**
 * Controller para o ponto de entrada (root) da API.
 * <p>
 * Este endpoint ({@code /api/v1}) serve como o "portão de entrada" para a API,
 * seguindo os princípios HATEOAS. Ele retorna os links para os principais
 * recursos (coleções), permitindo que os clientes descubram dinamicamente
 * as URLs disponíveis sem a necessidade de "chumbá-las" no código.
 */
@RestController
@RequestMapping("/api/v1")
public class ApiRootController {

    @GetMapping
    @Operation(summary = "Ponto de entrada da API", description = "Retorna os links para os principais recursos da API.")
    @ApiResponse(responseCode = "200", description = "Links de recursos retornados com sucesso.")
    public RepresentationModel<?> getRoot() {
        RepresentationModel<?> rootModel = new RepresentationModel<>();

        rootModel.add(linkTo(ProdutoController.class).withRel("produtos"));
        rootModel.add(linkTo(LoteMateriaPrimaController.class).withRel("lotes-materia-prima"));

        String tiposMateriaPrimaUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/tipos-materia-prima")
                .toUriString();
        rootModel.add(Link.of(tiposMateriaPrimaUrl, "tipos-materia-prima"));

        rootModel.add(linkTo(VendaController.class).withRel("vendas"));
        rootModel.add(linkTo(OrdemDeProducaoController.class).withRel("ordens-de-producao"));
        rootModel.add(linkTo(CanalVendaController.class).withRel("canais-venda"));
        
        String estoquesUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/estoques")
                .toUriString();
        rootModel.add(Link.of(estoquesUrl, "estoques"));

        return rootModel;
    }
}
