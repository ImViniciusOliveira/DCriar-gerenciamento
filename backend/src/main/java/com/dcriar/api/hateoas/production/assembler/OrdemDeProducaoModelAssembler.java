package com.dcriar.api.hateoas.production.assembler;

import com.dcriar.api.controller.production.OrdemDeProducaoController;
import com.dcriar.api.dto.response.production.OrdemDeProducaoResponseDTO;
import com.dcriar.api.hateoas.production.model.OrdemDeProducaoModel;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Assembler para converter {@link OrdemDeProducaoResponseDTO} em {@link OrdemDeProducaoModel}
 * adicionando links HATEOAS.
 */
@Component
public class OrdemDeProducaoModelAssembler extends RepresentationModelAssemblerSupport<OrdemDeProducaoResponseDTO, OrdemDeProducaoModel> {

    /**
     * Construtor padrão.
     */
    public OrdemDeProducaoModelAssembler() {
        super(OrdemDeProducaoController.class, OrdemDeProducaoModel.class);
    }

    /**
     * Converte um {@link OrdemDeProducaoResponseDTO} em {@link OrdemDeProducaoModel},
     * adicionando links HATEOAS.
     * <p>
     * Links adicionados:
     * <ul>
     *   <li>Auto (self)</li>
     *   <li>Deletar (deletar-ordem-de-producao)</li>
     *   <li>Coleção (ordens-de-producao)</li>
     * </ul>
     * @param dto DTO de resposta da ordem de produção
     * @return Modelo HATEOAS enriquecido
     */
    @Override
    @NonNull
    public OrdemDeProducaoModel toModel(@NonNull OrdemDeProducaoResponseDTO dto) {
        OrdemDeProducaoModel model = instantiateModel(dto);

        model.setId(dto.getId());
        model.setProdutoId(dto.getProdutoId());
        model.setNomeProduto(dto.getNomeProduto());
        model.setLotesConsumidosIds(dto.getLotesConsumidosIds());
        model.setQuantidadeProduzida(dto.getQuantidadeProduzida());
        model.setModoCalculo(dto.getModoCalculo());
        model.setDataCriacao(dto.getDataCriacao());
        model.setDataAtualizacao(dto.getDataAtualizacao());
        model.setMotivo(dto.getMotivo());
        model.setLarguraFinalCm(dto.getLarguraFinalCm());
        model.setComprimentoFinalCm(dto.getComprimentoFinalCm());
        model.setRotacionado(dto.getRotacionado());
        model.setCortesRealizados(dto.getCortesRealizados());
        model.setDetalhesCorte(dto.getDetalhesCorte());

        // Adiciona links HATEOAS
        if (dto.getId() != null) {
            model.add(linkTo(methodOn(OrdemDeProducaoController.class).buscarPorId(dto.getId())).withSelfRel());
            model.add(linkTo(methodOn(OrdemDeProducaoController.class).excluir(dto.getId())).withRel("deletar-ordem-de-producao"));
        }
        model.add(linkTo(OrdemDeProducaoController.class).withRel("ordens-de-producao"));

        return model;
    }

    /**
     * Cria uma resposta HTTP 201 (Created) com o modelo HATEOAS e header Location apontando para o novo recurso.
     */
    public ResponseEntity<OrdemDeProducaoModel> toCreatedResponseEntity(@NonNull OrdemDeProducaoResponseDTO dto) {
        OrdemDeProducaoModel model = toModel(dto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(dto.getId())
                .toUri();

        return ResponseEntity.created(location).body(model);
    }
}
