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

    @Override
    @Transactional
    public CorteRealizadoResponseDTO findById(Long id) {
        CorteRealizado corte = corteRealizadoRepository.findById(id)
                .orElseThrow(() -> new CorteRealizadoNaoEncontradoException(id));
        return corteRealizadoMapper.toResponseDTO(corte);
    }

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

    @Override
    @Transactional
    public List<CorteRealizadoResponseDTO> findAll() {
        return corteRealizadoRepository.findAll()
                .stream()
                .map(corteRealizadoMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        corteRealizadoRepository.deleteById(id);
    }
}
