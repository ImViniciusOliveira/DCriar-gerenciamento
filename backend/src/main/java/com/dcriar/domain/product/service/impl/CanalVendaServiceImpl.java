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

    /**
     * {@inheritDoc}
     * <p>
     * A lógica de validação e criação é delegada ao método {@link CanalVenda#from(CanalVendaRequestDTO)}.
     * Garante que o nome do canal de venda não seja duplicado, lançando uma exceção do banco de dados
     * caso a constraint de unicidade seja violada.
     */
    @Override
    @Transactional
    public CanalVendaResponseDTO create(CanalVendaRequestDTO requestDTO) {
        CanalVenda canal = CanalVenda.from(requestDTO);
        CanalVenda salvo = canalVendaRepository.save(canal);
        return canalVendaMapper.toResponseDTO(salvo);
    }

    /**
     * {@inheritDoc}
     * <p>
     * A lógica de validação e atualização é delegada ao método {@link CanalVenda#updateFrom(CanalVendaRequestDTO)}.
     * Garante que o novo nome do canal de venda não seja duplicado.
     * @throws CanalVendaNaoEncontradoException se o canal de venda com o ID fornecido não for encontrado.
     */
    @Override
    @Transactional
    public CanalVendaResponseDTO update(Long id, CanalVendaRequestDTO requestDTO) {
        CanalVenda canal = canalVendaRepository.findById(id)
                .orElseThrow(() -> new CanalVendaNaoEncontradoException(id));
        canal.updateFrom(requestDTO);
        CanalVenda atualizado = canalVendaRepository.save(canal);
        return canalVendaMapper.toResponseDTO(atualizado);
    }

    /**
     * {@inheritDoc}
     * @throws CanalVendaNaoEncontradoException se o canal de venda com o ID fornecido não for encontrado.
     */
    @Override
    @Transactional(readOnly = true)
    public CanalVendaResponseDTO findById(Long id) {
        CanalVenda canal = canalVendaRepository.findById(id)
                .orElseThrow(() -> new CanalVendaNaoEncontradoException(id));
        return canalVendaMapper.toResponseDTO(canal);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<CanalVendaResponseDTO> findAll() {
        return canalVendaRepository.findAll().stream()
                .map(canalVendaMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     * @throws CanalVendaNaoEncontradoException se o canal de venda com o ID fornecido não for encontrado.
     */
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
