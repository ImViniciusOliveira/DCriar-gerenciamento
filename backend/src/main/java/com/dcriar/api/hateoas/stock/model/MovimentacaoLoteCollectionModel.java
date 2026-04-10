package com.dcriar.api.hateoas.stock.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.springframework.hateoas.CollectionModel;

import java.util.Collection;

@Getter
@Setter
public class MovimentacaoLoteCollectionModel extends CollectionModel<MovimentacaoLoteModel> {

    @Schema(description = "Quantidade total de movimentações registradas para o lote.", example = "12")
    private int totalMovimentacoes;

    public MovimentacaoLoteCollectionModel(Collection<MovimentacaoLoteModel> content, int totalMovimentacoes) {
        super(content);
        this.totalMovimentacoes = totalMovimentacoes;
    }
}
