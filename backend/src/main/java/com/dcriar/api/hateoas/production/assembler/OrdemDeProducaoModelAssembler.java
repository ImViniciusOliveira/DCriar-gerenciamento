package com.dcriar.api.hateoas.production.assembler;

import com.dcriar.api.controller.production.OrdemDeProducaoController;
import com.dcriar.api.dto.response.production.OrdemDeProducaoResponseDTO;
import com.dcriar.api.hateoas.production.model.OrdemDeProducaoModel;
import com.dcriar.api.mapper.production.OrdemDeProducaoMapper;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * Assembler principal para o recurso de Ordem de Produção.
 * Converte {@link OrdemDeProducaoResponseDTO} em {@link OrdemDeProducaoModel} e constrói as respostas HATEOAS.
 */
@Component
public class OrdemDeProducaoModelAssembler extends RepresentationModelAssemblerSupport<OrdemDeProducaoResponseDTO, OrdemDeProducaoModel> {

    private final OrdemDeProducaoMapper mapper;

    /**
     * Construtor que injeta as dependências necessárias e inicializa a superclasse corretamente.
     *
     * @param mapper O mapper para converter os dados do DTO para o Model, injetado pelo Spring.
     */
    public OrdemDeProducaoModelAssembler(OrdemDeProducaoMapper mapper) {
        super(OrdemDeProducaoController.class, OrdemDeProducaoModel.class);
        this.mapper = mapper;
    }

    /**
     * Converte um {@link OrdemDeProducaoResponseDTO} em {@link OrdemDeProducaoModel},
     * adicionando links HATEOAS.
     * <p>
     * Este método utiliza o padrão de atualização via {@code @MappingTarget} no mapper
     * para popular o modelo, evitando dependências circulares de compilação.
     * <p>
     * Links adicionados:
     * <ul>
     *   <li>Auto (self)</li>
     *   <li>Deletar (deletar-ordem-de-producao)</li>
     *   <li>Coleção (ordens-de-producao)</li>
     * </ul>
     *
     * @param dto DTO de resposta da ordem de produção
     * @return Modelo HATEOAS enriquecido
     */
    @Override
    @NonNull
    public OrdemDeProducaoModel toModel(@NonNull OrdemDeProducaoResponseDTO dto) {
        // Cria a instância do modelo HATEOAS
        OrdemDeProducaoModel model = instantiateModel(dto);
        
        // Delega a população dos campos para o Mapper, usando o padrão de atualização
        mapper.updateModelFromDto(dto, model);

        // Adiciona links de ação apenas se a ordem de produção já existir (tiver um ID)
        if (dto.getId() != null) {
            String selfUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/v1/ordens-de-producao/{id}")
                    .buildAndExpand(dto.getId())
                    .toUriString();
            model.add(Link.of(selfUrl).withSelfRel());

            String deletarUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/v1/ordens-de-producao/{id}")
                    .buildAndExpand(dto.getId())
                    .toUriString();
            model.add(Link.of(deletarUrl, "deletar-ordem-de-producao"));
        } else {
            // Links para o esqueleto de criação
            String simularCorteUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/v1/ordens-de-producao/simular/corte")
                    .toUriString();
            model.add(Link.of(simularCorteUrl, "simular-corte"));

            String simularConsumoDiretoUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/v1/ordens-de-producao/simular/consumo-direto")
                    .toUriString();
            model.add(Link.of(simularConsumoDiretoUrl, "simular-consumo-direto"));

            String criarOrdemCorteUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/v1/ordens-de-producao/corte")
                    .toUriString();
            model.add(Link.of(criarOrdemCorteUrl, "criar-ordem-corte"));

            String criarOrdemConsumoDiretoUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/v1/ordens-de-producao/consumo-direto")
                    .toUriString();
            model.add(Link.of(criarOrdemConsumoDiretoUrl, "criar-ordem-consumo-direto"));
        }
        
        // Adiciona link para a coleção de ordens de produção
        String colecaoUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/ordens-de-producao")
                .toUriString();
        model.add(Link.of(colecaoUrl, "ordens-de-producao"));

        return model;
    }

    /**
     * Cria uma resposta HTTP 201 (Created) com o modelo HATEOAS e o header 'Location'
     * apontando para a URL do novo recurso criado.
     *
     * @param dto O DTO do recurso que acabou de ser criado.

     * @return Um ResponseEntity com status 201 e o modelo do recurso no corpo.
     */
    public ResponseEntity<OrdemDeProducaoModel> toCreatedResponseEntity(@NonNull OrdemDeProducaoResponseDTO dto) {
        OrdemDeProducaoModel model = toModel(dto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(dto.getId())
                .toUri();

        return ResponseEntity.created(location).body(model);
    }

    /**
     * Cria uma resposta HTTP 200 (OK) com o modelo HATEOAS.
     * Utilizado para endpoints que retornam um recurso existente ou um template vazio.
     *
     * @param dto O DTO do recurso.
     * @return Um ResponseEntity com status 200 e o modelo do recurso no corpo.
     */
    public ResponseEntity<OrdemDeProducaoModel> toOkResponseEntity(@NonNull OrdemDeProducaoResponseDTO dto) {
        return ResponseEntity.ok(toModel(dto));
    }
}
