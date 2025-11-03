package com.dcriar.api.hateoas.enums.assembler;

import com.dcriar.api.controller.enums.StockEnumController;
import com.dcriar.api.hateoas.enums.model.UnidadeDeMedidaModel;
import com.dcriar.domain.stock.entity.enums.UnidadeDeMedida;
import org.springframework.beans.BeanUtils;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Assembler para converter o enum {@link UnidadeDeMedida} em um modelo HATEOAS {@link UnidadeDeMedidaModel}.
 * <p>
 * Este assembler é responsável por criar o modelo de representação e adicionar os links HATEOAS relevantes.
 */
@Component
public class UnidadeDeMedidaModelAssembler extends RepresentationModelAssemblerSupport<UnidadeDeMedida, UnidadeDeMedidaModel> {

    public UnidadeDeMedidaModelAssembler() {
        super(StockEnumController.class, UnidadeDeMedidaModel.class);
    }

    @Override
    @NonNull
    public UnidadeDeMedidaModel toModel(@NonNull UnidadeDeMedida unidadeDeMedida) {
        // 1. Instancia o modelo de representação.
        UnidadeDeMedidaModel model = instantiateModel(unidadeDeMedida);

        // 2. Copia as propriedades do enum (descricao, simbolo) para o modelo.
        BeanUtils.copyProperties(unidadeDeMedida, model);

        // 3. Define o nome do enum, que não é copiado automaticamente pelo BeanUtils.
        model.setName(unidadeDeMedida.name());

        // 4. Adiciona o link para o próprio recurso (self link).
        model.add(linkTo(methodOn(StockEnumController.class).getUnidadesDeMedida()).slash(unidadeDeMedida.name()).withSelfRel());

        return model;
    }
}
