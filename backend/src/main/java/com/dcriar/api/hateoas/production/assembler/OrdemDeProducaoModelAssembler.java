package com.dcriar.api.hateoas.production.assembler;

import com.dcriar.api.controller.production.OrdemDeProducaoController;
import com.dcriar.api.dto.response.production.OrdemDeProducaoResponseDTO;
import com.dcriar.api.hateoas.production.model.OrdemDeProducaoModel;
import com.dcriar.api.mapper.production.OrdemDeProducaoMapper;
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
 * Observações de estilo do projeto:
 * - Preferimos constructor injection sem a anotação {@code @Autowired} (Spring injeta automaticamente)
 * - Mantemos a lógica de conversão no Mapper quando disponível
 */
@Component
public class OrdemDeProducaoModelAssembler extends RepresentationModelAssemblerSupport<OrdemDeProducaoResponseDTO, OrdemDeProducaoModel> {

    private final OrdemDeProducaoMapper ordemDeProducaoMapper;

    /**
     * Construtor com injeção de dependências por construtor (sem @Autowired) — consistente com outros assemblers.
     */
    public OrdemDeProducaoModelAssembler(OrdemDeProducaoMapper ordemDeProducaoMapper) {
        super(OrdemDeProducaoController.class, OrdemDeProducaoModel.class);
        this.ordemDeProducaoMapper = ordemDeProducaoMapper;
    }

    @Override
    @NonNull
    public OrdemDeProducaoModel toModel(@NonNull OrdemDeProducaoResponseDTO dto) {
        // Delega a conversão para o Mapper — responsabilidade de transformar DTO em Model
        OrdemDeProducaoModel model = ordemDeProducaoMapper.toModel(dto);

        // Links consistentes com os outros assemblers do projeto
        model.add(linkTo(methodOn(OrdemDeProducaoController.class).buscarPorId(dto.getId())).withSelfRel());
        model.add(linkTo(methodOn(OrdemDeProducaoController.class).excluir(dto.getId())).withRel("deletar-ordem-de-producao"));
        model.add(linkTo(methodOn(OrdemDeProducaoController.class).listarTodas()).withRel("ordens-de-producao"));

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
