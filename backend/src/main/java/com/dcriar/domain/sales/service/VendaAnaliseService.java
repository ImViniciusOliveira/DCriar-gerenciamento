package com.dcriar.domain.sales.service;

import com.dcriar.api.dto.response.sales.VendaAnalisePorCanalItemResponseDTO;
import com.dcriar.api.dto.response.sales.VendaAnalisePorProdutoItemResponseDTO;
import com.dcriar.api.dto.response.sales.VendaAnaliseTotaisResponseDTO;
import com.dcriar.api.dto.response.sales.VendaAnaliseSerieTemporalResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface VendaAnaliseService {

    VendaAnaliseTotaisResponseDTO consultarTotais(LocalDate dataInicio, LocalDate dataFim);

    Page<VendaAnalisePorCanalItemResponseDTO> consultarPorCanal(LocalDate dataInicio, LocalDate dataFim, Pageable pageable);

    Page<VendaAnalisePorProdutoItemResponseDTO> consultarPorProduto(LocalDate dataInicio, LocalDate dataFim, Pageable pageable);

    VendaAnaliseSerieTemporalResponseDTO consultarSerieTemporal(LocalDate dataInicio, LocalDate dataFim);
}
