package com.dcriar.domain.production.service;

import com.dcriar.api.dto.request.production.*;
import com.dcriar.api.dto.response.production.OrdemDeConsumoResponseDTO;
import com.dcriar.api.dto.response.production.OrdemDeProducaoResponseDTO;
import com.dcriar.api.dto.response.production.SimulacaoConsumoResponseDTO;
import com.dcriar.api.dto.response.production.SimulacaoCorteResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interface que define o contrato para a lógica de negócio de Ordens de Produção.
 * <p>
 * Abstrai os processos de fabrico, que podem ser de diferentes tipos, como
 * corte geométrico ou consumo de insumos.
 */
public interface OrdemDeProducaoService {

    /**
     * Cria e processa uma nova ordem de produção do tipo CORTE.
     *
     * @param requestDTO O DTO com os detalhes da ordem de corte.
     * @return Um DTO com os dados da ordem de produção criada.
     */
    OrdemDeProducaoResponseDTO criarOrdemDeCorte(OrdemDeCorteRequestDTO requestDTO);

    /**
     * Cria e processa uma nova ordem de produção do tipo CONSUMO.
     *
     * @param requestDTO O DTO com os detalhes da ordem de consumo.
     * @return Um DTO com os dados da ordem de produção criada.
     */
    OrdemDeConsumoResponseDTO criarOrdemDeConsumo(OrdemDeConsumoRequestDTO requestDTO);

    /**
     * Exclui uma ordem de produção pelo seu ID.
     *
     * @param id O ID da ordem a ser excluída.
     */
    void excluir(Long id);

    /**
     * Busca uma ordem de produção pelo seu ID.
     *
     * @param id O ID da ordem a ser buscada.
     * @return O DTO de resposta da ordem de produção encontrada.
     */
    OrdemDeProducaoResponseDTO buscarPorId(Long id);

    /**
     * Lista as ordens de produção de forma paginada.
     *
     * @param pageable Objeto com as informações de paginação (página, tamanho, ordenação).
     * @return Uma página de DTOs de resposta contendo as ordens de produção.
     */
    Page<OrdemDeProducaoResponseDTO> listarPaginado(Pageable pageable);

    /**
     * Simula uma produção baseada em corte, calculando o tamanho final do material
     * e o consumo estimado.
     *
     * @param requestDTO O DTO com o ID do produto e a quantidade a ser simulada.
     * @return Um DTO com os resultados da simulação de corte.
     */
    SimulacaoCorteResponseDTO simularCorte(SimulacaoCorteRequestDTO requestDTO);

    /**
     * Verifica e valida um layout de corte editado manualmente pelo usuário.
     *
     * @param requestDTO O DTO com os dados editados (margens, dimensões, etc.).
     * @return Um DTO com os resultados do novo layout calculado.
     */
    SimulacaoCorteResponseDTO verificarCorte(VerificacaoCorteRequestDTO requestDTO);

    /**
     * Simula uma produção baseada em consumo (ex: líquidos, pós, unidades),
     * calculando o consumo total estimado de matéria-prima.
     *
     * @param requestDTO O DTO com o ID do produto e a quantidade a ser simulada.
     * @return Um DTO com os resultados da simulação de consumo.
     */
    SimulacaoConsumoResponseDTO simularConsumo(SimulacaoConsumoRequestDTO requestDTO);
}
