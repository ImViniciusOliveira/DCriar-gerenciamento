package com.dcriar.api.hateoas.product.assembler;

import com.dcriar.api.controller.enums.StockEnumController;
import com.dcriar.api.controller.product.EstoqueProdutoController;
import com.dcriar.api.controller.product.ProdutoController;
import com.dcriar.api.controller.stock.TipoMateriaPrimaController;
import com.dcriar.api.dto.response.product.ProdutoResponseDTO;
import com.dcriar.api.hateoas.product.model.ProdutoModel;
import com.dcriar.api.mapper.product.ProdutoMapper;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

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
        ProdutoModel model = mapper.toModel(dto);

        // Links de descoberta para recursos relacionados, necessários para preencher formulários no frontend.
        model.add(linkTo(ProdutoController.class).withRel("produtos"));
        model.add(linkTo(methodOn(TipoMateriaPrimaController.class).findAll()).withRel("buscar-tipos-materia-prima"));
        model.add(linkTo(methodOn(StockEnumController.class).getUnidadesDeMedida()).withRel("unidades-de-medida"));

        // Adiciona links específicos do recurso apenas se o produto já existir (tiver um ID)
        if (dto.getId() != null) {
            model.add(linkTo(methodOn(ProdutoController.class).findById(dto.getId())).withSelfRel());
            model.add(linkTo(methodOn(ProdutoController.class).update(dto.getId(), new com.dcriar.api.dto.request.product.ProdutoRequestDTO())).withRel("atualizar-produto"));
            model.add(linkTo(methodOn(ProdutoController.class).patch(dto.getId(), new java.util.HashMap<>())).withRel("atualizar-parcialmente-produto"));
            model.add(linkTo(methodOn(ProdutoController.class).deleteById(dto.getId())).withRel("deletar-produto"));
            model.add(linkTo(methodOn(ProdutoController.class).uploadFoto(dto.getId(), null)).withRel("upload-foto"));

            // Pega o nome do arquivo (ex: "uuid_foto.jpg") que veio do Service
            String fileName = dto.getFotoPrincipalUrl();
            
            if (fileName != null && !fileName.isBlank()) {
                // Monta a URL absoluta: http://localhost:8080/api/v1/uploads/uuid_foto.jpg
                String fullUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/api/v1/uploads/")
                        .path(fileName)
                        .toUriString();
                
                // Define a URL pronta para o Frontend usar
                model.setFotoPrincipalUrl(fullUrl);
            }

            model.add(linkTo(methodOn(EstoqueProdutoController.class).listarMovimentacoesPorProduto(dto.getId())).withRel("historico-movimentacoes"));
        }

        return model;
    }

    @Override
    @NonNull
    public CollectionModel<ProdutoModel> toCollectionModel(@NonNull Iterable<? extends ProdutoResponseDTO> dtos) {
        CollectionModel<ProdutoModel> collectionModel = super.toCollectionModel(dtos);
        collectionModel.add(linkTo(methodOn(ProdutoController.class).getNewProductTemplate()).withRel("novo-produto"));

        // Adiciona o link de descoberta para o endpoint otimizado de busca de estoques por múltiplos produtos.
        // Usamos um UriComponentsBuilder para criar um link template, que é a forma correta para endpoints com @RequestParam.
        URI uri = UriComponentsBuilder.fromUri(linkTo(methodOn(EstoqueProdutoController.class).listarEstoquesPorListaDeProdutos(null)).toUri())
                .replaceQuery(null) // Remove a query gerada pelo HATEOAS com valor nulo
                .queryParam("produtoIds", "{ids}") // Adiciona um template de variável
                .build(true) // O 'true' indica que é um template
                .toUri();

        collectionModel.add(Link.of(uri.toString(), "estoques-por-produtos"));

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
