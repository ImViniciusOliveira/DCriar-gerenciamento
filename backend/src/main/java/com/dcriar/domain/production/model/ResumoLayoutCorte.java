package com.dcriar.domain.production.model;

import com.dcriar.api.dto.response.production.CorteRealizadoResponseDTO;
import lombok.Builder;

import java.util.List;

/**
 * Representa o resultado detalhado de uma simulação ou cálculo de layout de corte.
 * <p>
 * Este modelo de domínio transporta tanto a lista detalhada de cortes (para persistência)
 * quanto os dados resumidos do layout (para exibição no frontend).
 *
 * @param cortes                Lista detalhada de todos os cortes de produtos e retalhos gerados.
 * @param produtosPorLinha      Quantidade máxima de produtos que cabem em uma única linha.
 * @param numeroLinhasCompletas Quantidade de linhas que estão totalmente preenchidas com produtos.
 * @param produtosNaUltimaLinha Quantidade de produtos presentes na última linha (pode ser parcial).
 * @param sobraLateral          Descrição formatada da tira de sobra contínua (r1).
 * @param sobraInferior         Descrição formatada do bloco de sobra na última linha (r2).
 * @param saldoRolo             Descrição formatada da sobra de comprimento do rolo.
 */
@Builder
public record ResumoLayoutCorte(
        List<CorteRealizadoResponseDTO> cortes,
        int produtosPorLinha,
        int numeroLinhasCompletas,
        int produtosNaUltimaLinha,
        String sobraLateral,
        String sobraInferior,
        String saldoRolo
) {
}
