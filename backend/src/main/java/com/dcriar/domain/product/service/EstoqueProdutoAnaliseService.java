package com.dcriar.domain.product.service;

import com.dcriar.api.dto.response.product.AnaliseEstoqueProdutoResponseDTO;
import com.dcriar.domain.product.entity.enums.StatusAnaliseProduto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EstoqueProdutoAnaliseService {

    Page<AnaliseEstoqueProdutoResponseDTO> listarAnalise(
            Long produtoId,
            String nome,
            String tipoProduto,
            StatusAnaliseProduto statusAnalise,
            Pageable pageable
    );
}
