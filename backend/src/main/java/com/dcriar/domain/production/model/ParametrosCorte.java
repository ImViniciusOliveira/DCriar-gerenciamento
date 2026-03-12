package com.dcriar.domain.production.model;

import lombok.Builder;

import java.math.BigDecimal;

/**
 * Representa um objeto de valor imutável que armazena os parâmetros e resultados
 * dos cálculos intermediários para uma ordem de corte.
 * <p>
 * Esta classe não é uma entidade persistida, mas sim um modelo de dados temporário
 * usado para transportar informações complexas de forma organizada dentro da camada de serviço.
 *
 * @param larguraTotalLoteCm A largura total em centímetros do lote de matéria-prima.
 * @param larguraProduto     A largura unitária em centímetros do produto a ser cortado (na orientação final).
 * @param comprimentoProduto O comprimento unitário em centímetros do produto a ser cortado (na orientação final).
 * @param quantidade         A quantidade total de produtos a serem produzidos.
 * @param margemEsquerda     A margem esquerda solicitada pelo usuário.
 * @param margemDireita      A margem direita solicitada pelo usuário.
 * @param produtosPorLinha   O número de produtos que cabem em uma única linha no layout ótimo.
 * @param rotacionado        Indica se a orientação do produto foi rotacionada para otimização.
 * @param larguraBlocoProdutosCm A largura final do bloco de produtos, após aplicar as margens laterais.
 * @param larguraRetalhoLateralCm A largura final do retalho lateral (r1), após aplicar as margens.
 */
@Builder
public record ParametrosCorte(
    BigDecimal larguraTotalLoteCm,
    BigDecimal larguraProduto,
    BigDecimal comprimentoProduto,
    int quantidade,
    BigDecimal margemEsquerda,
    BigDecimal margemDireita,
    int produtosPorLinha,
    boolean rotacionado,
    BigDecimal larguraBlocoProdutosCm,
    BigDecimal larguraRetalhoLateralCm
) {}
