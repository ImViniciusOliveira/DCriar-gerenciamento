package com.dcriar.api.hateoas.stock.assembler;

import com.dcriar.api.controller.enums.StockEnumController;
import com.dcriar.api.controller.stock.LoteMateriaPrimaController;
import com.dcriar.api.controller.stock.TipoMateriaPrimaController;
import com.dcriar.api.dto.request.stock.LoteMateriaPrimaRequestDTO;
import com.dcriar.api.dto.request.stock.MovimentacaoRequestDTO;
import com.dcriar.api.dto.response.stock.LoteMateriaPrimaResponseDTO;
import com.dcriar.api.hateoas.stock.model.LoteMateriaPrimaModel;
import com.dcriar.api.mapper.stock.LoteMateriaPrimaMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Assembler para converter {@link LoteMateriaPrimaResponseDTO} em {@link LoteMateriaPrimaModel}
 * e construir as respostas HATEOAS para o controller.
 */
@Component
public class LoteMateriaPrimaModelAssembler extends RepresentationModelAssemblerSupport<LoteMateriaPrimaResponseDTO, LoteMateriaPrimaModel> {

    private final LoteMateriaPrimaMapper mapper;

    /**
     * Construtor que injeta as dependências necessárias.
     * @param mapper O mapper para converter os dados do DTO para o Model.
     */
    public LoteMateriaPrimaModelAssembler(LoteMateriaPrimaMapper mapper) {
        super(LoteMateriaPrimaController.class, LoteMateriaPrimaModel.class);
        this.mapper = mapper;
    }

    @Override
    @NonNull
    public LoteMateriaPrimaModel toModel(@NonNull LoteMateriaPrimaResponseDTO dto) {
        LoteMateriaPrimaModel model = instantiateModel(dto);
        
        // Delega a população dos campos para o Mapper
        mapper.updateModelFromDto(dto, model);

        // Links padrão para um lote existente
        if (dto.getId() != null) {
            model.add(linkTo(methodOn(LoteMateriaPrimaController.class).findById(dto.getId())).withSelfRel());
            
            // CORREÇÃO: Passa o DTO de Requisição correto para o método de update
            model.add(linkTo(methodOn(LoteMateriaPrimaController.class).update(dto.getId(), new LoteMateriaPrimaRequestDTO())).withRel("update"));
            model.add(linkTo(methodOn(LoteMateriaPrimaController.class).delete(dto.getId())).withRel("delete"));
            model.add(linkTo(methodOn(LoteMateriaPrimaController.class).listarMovimentacoes(dto.getId())).withRel("movimentacoes"));
            
            model.add(linkTo(methodOn(LoteMateriaPrimaController.class).registrarMovimentacao(dto.getId(), new MovimentacaoRequestDTO())).withRel("registrar-movimentacao"));

            if (model.getTipoMateriaPrimaId() != null) {
                model.add(linkTo(methodOn(TipoMateriaPrimaController.class).findById(model.getTipoMateriaPrimaId())).withRel("tipo-materia-prima"));
            }
            if (model.getLoteDeOrigemId() != null) {
                model.add(linkTo(methodOn(LoteMateriaPrimaController.class).findById(model.getLoteDeOrigemId())).withRel("lote-de-origem"));
            }
        } else {
            // Links para o esqueleto de criação
            model.add(linkTo(LoteMateriaPrimaController.class).withRel("create"));
            model.add(linkTo(TipoMateriaPrimaController.class).withRel("tipos-materia-prima"));
        }
        
        // Link para unidades de medida (necessário tanto para criação quanto para edição)
        model.add(linkTo(methodOn(StockEnumController.class).getUnidadesDeMedida()).withRel("unidades-de-medida"));

        return model;
    }

    public Page<LoteMateriaPrimaModel> toModel(Page<LoteMateriaPrimaResponseDTO> page) {
        List<LoteMateriaPrimaModel> content = page.getContent().stream()
                .map(this::toModel)
                .collect(Collectors.toList());
        return new PageImpl<>(content, page.getPageable(), page.getTotalElements());
    }

    public ResponseEntity<LoteMateriaPrimaModel> toCreatedResponseEntity(@NonNull LoteMateriaPrimaResponseDTO dto) {
        LoteMateriaPrimaModel model = toModel(dto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(dto.getId())
                .toUri();
        return ResponseEntity.created(location).body(model);
    }

    public ResponseEntity<LoteMateriaPrimaModel> toOkResponseEntity(@NonNull LoteMateriaPrimaResponseDTO dto) {
        LoteMateriaPrimaModel model = toModel(dto);
        return ResponseEntity.ok(model);
    }
}
