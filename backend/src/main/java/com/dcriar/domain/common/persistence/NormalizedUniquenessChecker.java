package com.dcriar.domain.common.persistence;

public interface NormalizedUniquenessChecker {

    boolean existsCanalVendaNome(String nome);

    boolean existsCanalVendaNome(Long excludingId, String nome);

    boolean existsTipoMateriaPrimaNome(String nome);

    boolean existsTipoMateriaPrimaNome(Long excludingId, String nome);

    boolean existsProdutoNome(String nome);

    boolean existsProdutoNome(Long excludingId, String nome);

    boolean existsProdutoSku(String sku);

    boolean existsProdutoSku(Long excludingId, String sku);
}
