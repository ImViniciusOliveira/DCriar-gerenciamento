package com.dcriar.domain.production.service.impl;

import com.dcriar.api.dto.request.production.CorteRealizadoRequestDTO;
import com.dcriar.api.dto.response.production.CorteRealizadoResponseDTO;
import com.dcriar.api.mapper.production.CorteRealizadoMapper;
import com.dcriar.domain.production.entity.CorteRealizado;
import com.dcriar.domain.production.entity.OrdemDeProducao;
import com.dcriar.domain.production.repository.CorteRealizadoRepository;
import com.dcriar.domain.production.repository.OrdemDeProducaoRepository;
import com.dcriar.domain.production.service.CorteRealizadoService;
import com.dcriar.exception.custom.CorteRealizadoNaoEncontradoException;
import com.dcriar.exception.custom.OrdemDeProducaoNaoEncontradaException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementação das operações de negócio para {@link CorteRealizado}.
 * <p>
 * Este serviço gerencia o ciclo de vida dos registros de cortes, que são sempre
 * vinculados a uma {@link OrdemDeProducao} pai.
 * <p>
 * A lógica de criação e atualização é delegada aos métodos {@code from} e {@code updateFrom}
 * da entidade para centralizar as regras de construção do objeto.
 */
@Service
@RequiredArgsConstructor
public class CorteRealizadoServiceImpl implements CorteRealizadoService {

    private final CorteRealizadoRepository corteRealizadoRepository;
    private final OrdemDeProducaoRepository ordemDeProducaoRepository;
    private final CorteRealizadoMapper corteRealizadoMapper;

    /**
     * Cria e associa um novo registro de CorteRealizado a uma Ordem de Produção.
     * <p>
     * A lógica de construção da entidade é delegada ao método {@link CorteRealizado#from(CorteRealizadoRequestDTO, OrdemDeProducao)}.
     *
     * @param dto DTO com os dados do corte realizado.
     * @return DTO de resposta do corte realizado cadastrado.
     * @throws OrdemDeProducaoNaoEncontradaException se a ordem de produção especificada no DTO não for encontrada.
     */
    @Override
    @Transactional
    public CorteRealizadoResponseDTO create(CorteRealizadoRequestDTO dto) {
        // 1. Valida e busca a Ordem de Produção pai.
        OrdemDeProducao ordem = ordemDeProducaoRepository.findById(dto.getOrdemDeProducaoId())
                .orElseThrow(() -> new OrdemDeProducaoNaoEncontradaException(dto.getOrdemDeProducaoId()));
        
        // 2. Cria a entidade CorteRealizado e a salva.
        CorteRealizado corte = CorteRealizado.from(dto, ordem);
        CorteRealizado salvo = corteRealizadoRepository.save(corte);
        return corteRealizadoMapper.toResponseDTO(salvo);
    }

    /**
     * Atualiza um registro existente de CorteRealizado.
     * <p>
     * <b>Regra de Negócio:</b> É possível reassociar um corte a uma Ordem de Produção diferente,
     * embora isso possa levar a inconsistências se não for feito com cuidado.
     * <p>
     * A lógica de atualização é delegada ao método {@link CorteRealizado#updateFrom(CorteRealizadoRequestDTO, OrdemDeProducao)}.
     *
     * @param id  ID do corte realizado a ser atualizado.
     * @param dto DTO com os dados para atualização.
     * @return DTO de resposta do corte realizado atualizado.
     * @throws CorteRealizadoNaoEncontradoException se o corte não for encontrado.
     * @throws OrdemDeProducaoNaoEncontradaException se a nova ordem de produção não for encontrada.
     */
    @Override
    @Transactional
    public CorteRealizadoResponseDTO update(Long id, CorteRealizadoRequestDTO dto) {
        CorteRealizado corte = corteRealizadoRepository.findById(id)
                .orElseThrow(() -> new CorteRealizadoNaoEncontradoException(id));
        OrdemDeProducao ordem = ordemDeProducaoRepository.findById(dto.getOrdemDeProducaoId())
                .orElseThrow(() -> new OrdemDeProducaoNaoEncontradaException(dto.getOrdemDeProducaoId()));
        
        corte.updateFrom(dto, ordem);
        CorteRealizado atualizado = corteRealizadoRepository.save(corte);
        return corteRealizadoMapper.toResponseDTO(atualizado);
    }

    /**
     * Consulta um corte realizado pelo ID.
     *
     * @param id ID do corte realizado.
     * @return DTO de resposta do corte realizado encontrado.
     * @throws CorteRealizadoNaoEncontradoException se o corte não for encontrado.
     */
    @Override
    @Transactional
    public CorteRealizadoResponseDTO findById(Long id) {
        CorteRealizado corte = corteRealizadoRepository.findById(id)
                .orElseThrow(() -> new CorteRealizadoNaoEncontradoException(id));
        return corteRealizadoMapper.toResponseDTO(corte);
    }

    /**
     * Lista todos os cortes realizados de uma ordem de produção específica.
     *
     * @param ordemDeProducaoId ID da ordem de produção.
     * @return Lista de DTOs de resposta dos cortes realizados.
     * @throws OrdemDeProducaoNaoEncontradaException se a ordem de produção não for encontrada.
     */
    @Override
    @Transactional
    public List<CorteRealizadoResponseDTO> findByOrdemDeProducao(Long ordemDeProducaoId) {
        OrdemDeProducao ordem = ordemDeProducaoRepository.findById(ordemDeProducaoId)
                .orElseThrow(() -> new OrdemDeProducaoNaoEncontradaException(ordemDeProducaoId));
        return corteRealizadoRepository.findByOrdemDeProducao(ordem)
                .stream()
                .map(corteRealizadoMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Lista todos os cortes realizados cadastrados no sistema.
     *
     * @return Lista de DTOs de resposta de todos os cortes realizados.
     */
    @Override
    @Transactional
    public List<CorteRealizadoResponseDTO> findAll() {
        return corteRealizadoRepository.findAll()
                .stream()
                .map(corteRealizadoMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Exclui um corte realizado pelo seu ID.
     * <p>
     * <b>Atenção:</b> Esta é uma operação de exclusão física (hard delete) que não realiza
     * validações ou atualizações em cascata. A exclusão de um corte <strong>não</strong>
     * recalcula os totais ou o estado da {@link OrdemDeProducao} pai, o que pode
     * levar a inconsistências nos dados da ordem de produção.
     *
     * @param id ID do corte realizado a ser excluído.
     */
    @Override
    @Transactional
    public void delete(Long id) {
        corteRealizadoRepository.deleteById(id);
    }
}
