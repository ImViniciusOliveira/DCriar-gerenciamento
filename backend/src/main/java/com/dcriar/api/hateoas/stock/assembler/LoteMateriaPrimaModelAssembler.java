package com.dcriar.api.hateoas.stock.assembler;

import com.dcriar.api.controller.enums.StockEnumController;
import com.dcriar.api.controller.stock.LoteMateriaPrimaController;
import com.dcriar.api.controller.stock.TipoMateriaPrimaController;
import com.dcriar.api.dto.response.stock.LoteMateriaPrimaResponseDTO;
import com.dcriar.api.hateoas.stock.model.LoteMateriaPrimaModel;
import org.springframework.beans.BeanUtils;
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

    public LoteMateriaPrimaModelAssembler() {
        super(LoteMateriaPrimaController.class, LoteMateriaPrimaModel.class);
    }

    @Override
    @NonNull
    public LoteMateriaPrimaModel toModel(@NonNull LoteMateriaPrimaResponseDTO dto) {
        LoteMateriaPrimaModel model = instantiateModel(dto);
        BeanUtils.copyProperties(dto, model);

        // Links padrão para um lote existente
        if (dto.getId() != null) {
            model.add(linkTo(methodOn(LoteMateriaPrimaController.class).findById(dto.getId())).withSelfRel());
            // Para update e delete, usamos o ID do próprio lote
            model.add(linkTo(methodOn(LoteMateriaPrimaController.class).update(dto.getId(), null)).withRel("update"));
            model.add(linkTo(methodOn(LoteMateriaPrimaController.class).delete(dto.getId())).withRel("delete"));
            model.add(linkTo(methodOn(LoteMateriaPrimaController.class).listarMovimentacoes(dto.getId())).withRel("movimentacoes"));
            // Para registrar movimentação, também precisamos do ID do lote
            model.add(linkTo(methodOn(LoteMateriaPrimaController.class).registrarMovimentacao(dto.getId(), null)).withRel("registrar-movimentacao"));

            if (model.getTipoMateriaPrimaId() != null) {
                model.add(linkTo(methodOn(TipoMateriaPrimaController.class).findById(model.getTipoMateriaPrimaId())).withRel("tipo-materia-prima"));
            }
            if (model.getLoteDeOrigemId() != null) {
                model.add(linkTo(methodOn(LoteMateriaPrimaController.class).findById(model.getLoteDeOrigemId())).withRel("lote-de-origem"));
            }
        } else {
            // Links para o esqueleto de criação
            model.add(linkTo(LoteMateriaPrimaController.class).withRel("create"));
            model.add(linkTo(methodOn(TipoMateriaPrimaController.class).findAll()).withRel("tipos-materia-prima"));
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
