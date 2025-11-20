package com.dcriar.domain.product.service.impl;

import com.dcriar.api.dto.request.product.CanalVendaRequestDTO;
import com.dcriar.api.dto.response.product.CanalVendaResponseDTO;
import com.dcriar.api.mapper.product.CanalVendaMapper;
import com.dcriar.domain.product.entity.CanalVenda;
import com.dcriar.domain.product.repository.CanalVendaRepository;
import com.dcriar.domain.product.service.CanalVendaService;
import com.dcriar.exception.custom.CanalVendaNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementação da lógica de negócio para CanalVenda.
 */
@Service
@RequiredArgsConstructor
public class CanalVendaServiceImpl implements CanalVendaService {

    private final CanalVendaRepository canalVendaRepository;
    private final CanalVendaMapper canalVendaMapper;

    @Override
    @Transactional
    public CanalVendaResponseDTO create(CanalVendaRequestDTO requestDTO) {
        CanalVenda canal = CanalVenda.from(requestDTO);
        CanalVenda salvo = canalVendaRepository.save(canal);
        return canalVendaMapper.toResponseDTO(salvo);
    }

    @Override
    @Transactional
    public CanalVendaResponseDTO update(Long id, CanalVendaRequestDTO requestDTO) {
        CanalVenda canal = canalVendaRepository.findById(id)
                .orElseThrow(() -> new CanalVendaNaoEncontradoException(id));
        canal.updateFrom(requestDTO);
        CanalVenda atualizado = canalVendaRepository.save(canal);
        return canalVendaMapper.toResponseDTO(atualizado);
    }

    @Override
    @Transactional(readOnly = true)
    public CanalVendaResponseDTO findById(Long id) {
        CanalVenda canal = canalVendaRepository.findById(id)
                .orElseThrow(() -> new CanalVendaNaoEncontradoException(id));
        return canalVendaMapper.toResponseDTO(canal);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CanalVendaResponseDTO> findAll() {
        return canalVendaRepository.findAll().stream()
                .map(canalVendaMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        if (!canalVendaRepository.existsById(id)) {
            throw new CanalVendaNaoEncontradoException(id);
        }
        // TODO: Adicionar validação para impedir exclusão se o canal estiver em uso (ex: em Estoques)
        canalVendaRepository.deleteById(id);
    }
}
