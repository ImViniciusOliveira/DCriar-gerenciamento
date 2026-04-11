package com.dcriar.domain.stock.service;

import com.dcriar.api.dto.request.stock.PoliticaSaldoRetalhoAnaliseFiltro;
import com.dcriar.api.dto.response.stock.AnaliseEstoqueMateriaPrimaResponseDTO;
import com.dcriar.domain.stock.entity.enums.StatusAnaliseMateriaPrima;
import com.dcriar.domain.stock.entity.enums.UnidadeDeMedida;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EstoqueMateriaPrimaAnaliseService {

    Page<AnaliseEstoqueMateriaPrimaResponseDTO> listarAnalise(
            Long tipoMateriaPrimaId,
            String nome,
            UnidadeDeMedida unidadeDeConsumo,
            String tipoProduto,
            StatusAnaliseMateriaPrima statusAnalise,
            PoliticaSaldoRetalhoAnaliseFiltro politicaSaldoRetalho,
            Pageable pageable
    );
}
