package com.dcriar.api.hateoas.product.assembler;

import com.dcriar.api.controller.enums.StockEnumController;
import com.dcriar.api.controller.product.EstoqueProdutoController;
import com.dcriar.api.controller.product.ProdutoController;
import com.dcriar.api.controller.production.OrdemDeProducaoController;
import com.dcriar.api.controller.stock.TipoMateriaPrimaController;
import com.dcriar.api.dto.response.product.ProdutoDeConsumoDiretoResponseDTO;
import com.dcriar.api.dto.response.product.ProdutoDeCorteResponseDTO;
import com.dcriar.api.dto.response.product.ProdutoResponseDTO;
import com.dcriar.api.hateoas.product.model.ProdutoDeConsumoDiretoModel;
import com.dcriar.api.hateoas.product.model.ProdutoDeCorteModel;
import com.dcriar.api.hateoas.product.model.ProdutoModel;
import com.dcriar.api.mapper.product.ProdutoMapper;
import org.springframework.hateoas.*;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Assembler responsável por converter {@link ProdutoResponseDTO} em {@link ProdutoModel},
 * adicionando links HATEOAS para navegação entre endpoints relacionados ao produto.
 */
@Component
public class ProdutoModelAssembler extends RepresentationModelAssemblerSupport<ProdutoResponseDTO, ProdutoModel> {

    private final ProdutoMapper mapper;

    public ProdutoModelAssembler(ProdutoMapper mapper) {
        super(ProdutoController.class, ProdutoModel.class);
        this.mapper = mapper;
    }

    @Override
    @NonNull
    public ProdutoModel toModel(@NonNull ProdutoResponseDTO dto) {
        // Instancia o tipo de Model correto baseado no tipo do DTO
        ProdutoModel model = instantiateCorrectModel(dto);

        // Delega a população dos campos para o Mapper usando o padrão de atualização
        mapper.updateModelFromDto(dto, model);

        // Links de descoberta para recursos relacionados, necessários para preencher formulários no frontend.
        model.add(linkTo(ProdutoController.class).withRel("produtos"));
        model.add(linkTo(TipoMateriaPrimaController.class).withRel("buscar-tipos-materia-prima"));
        model.add(linkTo(methodOn(StockEnumController.class).getUnidadesDeMedida()).withRel("unidades-de-medida"));

        // Adiciona links específicos do recurso apenas se o produto já existir (tiver um ID)
        if (dto.getId() != null) {
            model.add(linkTo(methodOn(ProdutoController.class).findById(dto.getId())).withSelfRel());
            model.add(linkTo(methodOn(ProdutoController.class).update(dto.getId(), new com.dcriar.api.dto.request.product.ProdutoRequestDTO())).withRel("atualizar-produto"));
            model.add(linkTo(methodOn(ProdutoController.class).patch(dto.getId(), new java.util.HashMap<>())).withRel("atualizar-parcialmente-produto"));
            model.add(linkTo(methodOn(ProdutoController.class).deleteById(dto.getId())).withRel("deletar-produto"));
            model.add(linkTo(methodOn(ProdutoController.class).uploadFoto(dto.getId(), null)).withRel("upload-foto"));

            // Adiciona o link de simulação apropriado com base no tipo de produto
            if ("CORTE".equals(dto.getTipoProduto())) {
                model.add(linkTo(methodOn(OrdemDeProducaoController.class)
                        .simularCorte(null)).withRel("simulate"));
            } else if ("CONSUMO_DIRETO".equals(dto.getTipoProduto())) {
                model.add(linkTo(methodOn(OrdemDeProducaoController.class)
                        .simularConsumoDireto(null)).withRel("simulate"));
            }

            String fileName = dto.getFotoPrincipalUrl();
            
            if (fileName != null && !fileName.isBlank()) {
                String fullUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/api/v1/uploads/")
                        .path(fileName)
                        .toUriString();
                model.setFotoPrincipalUrl(fullUrl);
            }

            model.add(linkTo(methodOn(EstoqueProdutoController.class).listarMovimentacoesPorProduto(dto.getId())).withRel("historico-movimentacoes"));
        }

        return model;
    }

    private ProdutoModel instantiateCorrectModel(ProdutoResponseDTO dto) {
        if (dto instanceof ProdutoDeCorteResponseDTO) {
            return new ProdutoDeCorteModel();
        } else if (dto instanceof ProdutoDeConsumoDiretoResponseDTO) {
            return new ProdutoDeConsumoDiretoModel();
        }
        throw new IllegalArgumentException("Tipo de DTO de produto não suportado pelo Assembler: " + dto.getClass().getName());
    }

    @Override
    @NonNull
    public CollectionModel<ProdutoModel> toCollectionModel(@NonNull Iterable<? extends ProdutoResponseDTO> dtos) {
        CollectionModel<ProdutoModel> collectionModel = super.toCollectionModel(dtos);
        collectionModel.add(linkTo(methodOn(ProdutoController.class).getNewProductTemplate()).withRel("novo-produto"));

        // Padrão ideal para links com template
        Link baseLink = linkTo(EstoqueProdutoController.class).withSelfRel();
        UriTemplate template = UriTemplate.of(baseLink.getHref()).with("produtoIds", TemplateVariable.VariableType.REQUEST_PARAM);
        collectionModel.add(Link.of(template, "estoques-por-produtos"));

        return collectionModel;
    }

    public ResponseEntity<ProdutoModel> toCreatedResponseEntity(@NonNull ProdutoResponseDTO dto) {
        ProdutoModel model = toModel(dto);

        return model.getLink("self")
                .map(link -> ResponseEntity.created(link.toUri()).body(model))
                .orElse(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }

    public ResponseEntity<ProdutoModel> toOkResponseEntity(@NonNull ProdutoResponseDTO dto) {
        return ResponseEntity.ok(toModel(dto));
    }
}
