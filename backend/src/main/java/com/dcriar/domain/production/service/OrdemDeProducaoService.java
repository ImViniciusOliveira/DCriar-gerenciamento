package com.dcriar.domain.production.service;

import com.dcriar.api.dto.request.production.OrdemDeConsumoDiretoRequestDTO;
import com.dcriar.api.dto.request.production.OrdemDeCorteRequestDTO;
import com.dcriar.api.dto.request.production.SimulacaoConsumoDiretoRequestDTO;
import com.dcriar.api.dto.request.production.SimulacaoCorteRequestDTO;
import com.dcriar.api.dto.response.production.OrdemDeProducaoResponseDTO;
import com.dcriar.api.dto.response.production.SimulacaoConsumoDiretoResponseDTO;
import com.dcriar.api.dto.response.production.SimulacaoCorteResponseDTO;

import java.util.List;

/**
 * Interface que define o contrato para a lógica de negócio de Ordens de Produção.
 * <p>
 * Abstrai os processos de fabrico, que podem ser de diferentes tipos, como
 * corte geométrico ou consumo direto de insumos.
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
     * Cria e processa uma nova ordem de produção do tipo CONSUMO DIRETO.
     *
     * @param requestDTO O DTO com os detalhes da ordem de consumo.
     * @return Um DTO com os dados da ordem de produção criada.
     */
    OrdemDeProducaoResponseDTO criarOrdemDeConsumoDireto(OrdemDeConsumoDiretoRequestDTO requestDTO);

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
     * Lista todas as ordens de produção registradas no sistema.
     *
     * @return Uma lista de DTOs de resposta contendo todas as ordens de produção.
     */
    List<OrdemDeProducaoResponseDTO> listarTodas();

    /**
     * Simula uma produção baseada em corte, calculando o tamanho final do material
     * e o consumo estimado.
     *
     * @param requestDTO O DTO com o ID do produto e a quantidade a ser simulada.
     * @return Um DTO com os resultados da simulação de corte.
     */
    SimulacaoCorteResponseDTO simularCorte(SimulacaoCorteRequestDTO requestDTO);

    /**
     * Simula uma produção baseada em consumo direto (ex: líquidos, pós, unidades),
     * calculando o consumo total estimado de matéria-prima.
     *
     * @param requestDTO O DTO com o ID do produto e a quantidade a ser simulada.
     * @return Um DTO com os resultados da simulação de consumo.
     */
    SimulacaoConsumoDiretoResponseDTO simularConsumoDireto(SimulacaoConsumoDiretoRequestDTO requestDTO);
}
