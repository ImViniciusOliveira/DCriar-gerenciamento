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
 * @param larguraProduto     A largura unitária em centímetros do produto a ser cortado.
 * @param comprimentoProduto O comprimento unitário em centímetros do produto a ser cortado.
 * @param quantidade         A quantidade total de produtos a serem produzidos.
 * @param margemEsquerda     A margem esquerda em centímetros a ser desconsiderada no lote.
 * @param margemDireita      A margem direita em centímetros a ser desconsiderada no lote.
 * @param larguraUtilCm      A largura útil calculada do lote (largura total - margens).
 * @param produtosPorLinha   O número de produtos que cabem em uma única linha dentro da largura útil.
 * @param rotacionado        Indica se a orientação do produto foi rotacionada para otimização.
 */
@Builder
public record ParametrosCorte(BigDecimal larguraTotalLoteCm, BigDecimal larguraProduto, BigDecimal comprimentoProduto,
                              int quantidade, BigDecimal margemEsquerda, BigDecimal margemDireita,
                              BigDecimal larguraUtilCm, int produtosPorLinha, boolean rotacionado) {

}
