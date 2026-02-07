package com.dcriar.api.hateoas.sales.assembler;

import com.dcriar.api.controller.sales.VendaController;
import com.dcriar.api.dto.request.sales.VendaRequestDTO;
import com.dcriar.api.dto.response.sales.VendaResponseDTO;
import com.dcriar.api.hateoas.sales.model.ItemVendaModel;
import com.dcriar.api.hateoas.sales.model.VendaModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Assembler principal para o recurso de Venda.
 * Converte {@link VendaResponseDTO} em {@link VendaModel} e constrói as respostas HATEOAS.
 */
@Component
public class VendaModelAssembler extends RepresentationModelAssemblerSupport<VendaResponseDTO, VendaModel> {

    private final ItemVendaModelAssembler itemVendaModelAssembler;

    /**
     * Construtor que injeta as dependências necessárias e inicializa a superclasse corretamente.
     *
     * @param itemVendaModelAssembler O assembler para os itens da venda, injetado pelo Spring.
     */
    @Autowired
    public VendaModelAssembler(ItemVendaModelAssembler itemVendaModelAssembler) {
        super(VendaController.class, VendaModel.class);
        this.itemVendaModelAssembler = itemVendaModelAssembler;
    }

    /**
     * Converte um {@link VendaResponseDTO} em {@link VendaModel},
     * adicionando links HATEOAS e montando os itens da venda.
     * <p>
     * Links adicionados:
     * <ul>
     *   <li>Auto (self)</li>
     *   <li>Atualizar (update)</li>
     *   <li>Deletar (delete)</li>
     *   <li>Coleção (vendas)</li>
     * </ul>
     * @param dto DTO de resposta da venda
     * @return Modelo HATEOAS enriquecido
     */
    @Override
    @NonNull
    public VendaModel toModel(@NonNull VendaResponseDTO dto) {
        VendaModel model = instantiateModel(dto);

        model.setId(dto.getId());
        model.setDataCriacao(dto.getDataCriacao());
        model.setDataAtualizacao(dto.getDataAtualizacao());
        model.setNomeCanalVenda(dto.getNomeCanalVenda());
        model.setValorTotal(dto.getValorTotal());
        model.setCanalVendaId(dto.getCanalVendaId());

        if (dto.getItens() != null) {
            List<ItemVendaModel> itemModels = dto.getItens().stream()
                    .map(itemVendaModelAssembler::toModel)
                    .collect(Collectors.toList());
            model.setItens(itemModels);
        } else {
            model.setItens(Collections.emptyList());
        }

        // Adiciona links de ação apenas se o ID existir
        if (dto.getId() != null) {
            model.add(linkTo(methodOn(VendaController.class).findById(dto.getId())).withSelfRel());
            model.add(linkTo(methodOn(VendaController.class).atualizarVenda(dto.getId(), new VendaRequestDTO())).withRel("update"));
            model.add(linkTo(methodOn(VendaController.class).deletarVenda(dto.getId())).withRel("delete"));
        }
        
        // Adiciona link para a coleção de vendas
        model.add(linkTo(methodOn(VendaController.class).findAll(null, null)).withRel("vendas"));

        return model;
    }

    public ResponseEntity<VendaModel> toCreatedResponseEntity(@NonNull VendaResponseDTO dto) {
        VendaModel model = toModel(dto);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(dto.getId())
                .toUri();

        return ResponseEntity.created(location).body(model);
    }

    /**
     * Envolve o modelo HATEOAS em um ResponseEntity com status 200 OK.
     * Útil para retornos de métodos GET (detalhes, templates).
     *
     * @param dto O DTO a ser convertido.
     * @return ResponseEntity contendo o modelo.
     */
    public ResponseEntity<VendaModel> toOkResponseEntity(@NonNull VendaResponseDTO dto) {
        return ResponseEntity.ok(toModel(dto));
    }
}
