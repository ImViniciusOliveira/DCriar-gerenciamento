package com.dcriar.exception.custom;

import lombok.Getter;

import java.util.Map;
import java.util.Set;

/**
 * Lançada quando uma atualização tenta alterar campos estruturais de um produto
 * que já possui uso operacional e, portanto, não deve mais ter esses dados modificados.
 */
@Getter
public class ProdutoCamposBloqueadosException extends AbstractCamposBloqueadosException {

    private final Long produtoId;
    private final String nomeProduto;

    public ProdutoCamposBloqueadosException(Long produtoId, String nomeProduto, Set<String> camposBloqueados, Map<String, String> motivosBloqueio) {
        super(String.format(
                "Não é possível alterar os campos estruturais do produto '%s' (%s). Motivos: %s",
                nomeProduto,
                formatarCampos(camposBloqueados),
                formatarMotivos(motivosBloqueio, camposBloqueados)
        ), camposBloqueados, motivosBloqueio);
        this.produtoId = produtoId;
        this.nomeProduto = nomeProduto;
    }
}
