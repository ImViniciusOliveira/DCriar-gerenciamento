package com.dcriar.api.hateoas.stock.assembler;

import com.dcriar.api.controller.stock.EstoqueMateriaPrimaController;
import com.dcriar.api.controller.stock.LoteMateriaPrimaController;
import com.dcriar.api.controller.stock.TipoMateriaPrimaController;
import com.dcriar.api.dto.request.stock.PoliticaSaldoRetalhoAnaliseFiltro;
import com.dcriar.api.dto.response.stock.AnaliseEstoqueMateriaPrimaResponseDTO;
import com.dcriar.api.hateoas.stock.model.AnaliseEstoqueMateriaPrimaModel;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class AnaliseEstoqueMateriaPrimaModelAssembler extends RepresentationModelAssemblerSupport<AnaliseEstoqueMateriaPrimaResponseDTO, AnaliseEstoqueMateriaPrimaModel> {

    public AnaliseEstoqueMateriaPrimaModelAssembler() {
        super(EstoqueMateriaPrimaController.class, AnaliseEstoqueMateriaPrimaModel.class);
    }

    @Override
    @NonNull
    public AnaliseEstoqueMateriaPrimaModel toModel(@NonNull AnaliseEstoqueMateriaPrimaResponseDTO dto) {
        AnaliseEstoqueMateriaPrimaModel model = AnaliseEstoqueMateriaPrimaModel.builder()
                .tipoMateriaPrimaId(dto.getTipoMateriaPrimaId())
                .nomeTipoMateriaPrima(dto.getNomeTipoMateriaPrima())
                .unidadeDeConsumo(dto.getUnidadeDeConsumo())
                .unidadeDescricao(dto.getUnidadeDescricao())
                .unidadeSimbolo(dto.getUnidadeSimbolo())
                .tipoProdutoCompativel(dto.getTipoProdutoCompativel())
                .saldoLotesPrincipais(dto.getSaldoLotesPrincipais())
                .saldoRetalhos(dto.getSaldoRetalhos())
                .saldoTotal(dto.getSaldoTotal())
                .saldoConsiderado(dto.getSaldoConsiderado())
                .quantidadeLotesPrincipais(dto.getQuantidadeLotesPrincipais())
                .quantidadeRetalhos(dto.getQuantidadeRetalhos())
                .estoqueCritico(dto.getEstoqueCritico())
                .estoqueAceitavel(dto.getEstoqueAceitavel())
                .percentualRisco(dto.getPercentualRisco())
                .statusAnalise(dto.getStatusAnalise())
                .build();

        model.add(linkTo(methodOn(EstoqueMateriaPrimaController.class)
                .listarAnaliseEstoque(
                        dto.getTipoMateriaPrimaId(),
                        null,
                        dto.getUnidadeDeConsumo(),
                        dto.getTipoProdutoCompativel(),
                        PoliticaSaldoRetalhoAnaliseFiltro.TODOS,
                        PageRequest.of(0, 10, Sort.by("nome").ascending()),
                        null
                ))
                .withSelfRel());
        model.add(linkTo(methodOn(TipoMateriaPrimaController.class).findById(dto.getTipoMateriaPrimaId())).withRel("tipo-materia-prima"));

        String lotesUri = linkTo(LoteMateriaPrimaController.class).toUri().toString();
        model.add(Link.of(UriComponentsBuilder.fromUriString(lotesUri)
                .queryParam("tipoMateriaPrimaId", dto.getTipoMateriaPrimaId())
                .build()
                .toUriString(), "lotes"));

        return model;
    }
}
