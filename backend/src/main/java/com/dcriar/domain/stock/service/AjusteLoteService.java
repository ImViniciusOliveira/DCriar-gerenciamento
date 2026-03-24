package com.dcriar.domain.stock.service;

import com.dcriar.api.dto.request.stock.CalcularAjusteLoteRequestDTO;
import com.dcriar.api.dto.response.stock.CalcularAjusteLoteResponseDTO;

/**
 * Contrato do fluxo operacional de ajuste de lote, separado das movimentações técnicas do sistema.
 */
public interface AjusteLoteService {

    /**
     * Calcula o impacto de um ajuste de lote sem persistir mudanças.
     *
     * @param loteId ID do lote ajustado.
     * @param requestDTO parâmetros operacionais do ajuste.
     * @return preview do ajuste.
     */
    CalcularAjusteLoteResponseDTO calcular(Long loteId, CalcularAjusteLoteRequestDTO requestDTO);
}
