package com.dcriar.domain.product.service.impl;

import com.dcriar.api.dto.request.product.CanalVendaRequestDTO;
import com.dcriar.api.dto.response.product.CanalVendaResponseDTO;
import com.dcriar.api.mapper.product.CanalVendaMapper;
import com.dcriar.domain.common.persistence.NormalizedUniquenessChecker;
import com.dcriar.domain.common.util.HumanTextNormalizer;
import com.dcriar.domain.common.util.UniqueComparisonNormalizer;
import com.dcriar.domain.product.entity.CanalVenda;
import com.dcriar.domain.product.entity.Estoque;
import com.dcriar.domain.product.repository.CanalVendaRepository;
import com.dcriar.domain.product.repository.EstoqueRepository;
import com.dcriar.domain.production.entity.OrdemDeProducao;
import com.dcriar.domain.production.repository.OrdemDeProducaoRepository;
import com.dcriar.domain.product.service.CanalVendaService;
import com.dcriar.domain.sales.entity.Venda;
import com.dcriar.domain.sales.repository.VendaRepository;
import com.dcriar.exception.custom.CanalVendaEmUsoException;
import com.dcriar.exception.custom.CanalVendaNomeDuplicadoException;
import com.dcriar.exception.custom.CanalVendaNaoEncontradoException;
import com.dcriar.exception.custom.AtualizacaoSemAlteracoesException;
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
    private final NormalizedUniquenessChecker normalizedUniquenessChecker;
    private final EstoqueRepository estoqueRepository;
    private final VendaRepository vendaRepository;
    private final OrdemDeProducaoRepository ordemDeProducaoRepository;

    @Override
    @Transactional
    public CanalVendaResponseDTO create(CanalVendaRequestDTO requestDTO) {
        validarNomeDisponivelParaCriacao(requestDTO.getNome());
        CanalVenda canal = CanalVenda.from(requestDTO);
        CanalVenda salvo = canalVendaRepository.save(canal);
        return canalVendaMapper.toResponseDTO(salvo);
    }

    @Override
    @Transactional
    public CanalVendaResponseDTO update(Long id, CanalVendaRequestDTO requestDTO) {
        CanalVenda canal = canalVendaRepository.findById(id)
                .orElseThrow(() -> new CanalVendaNaoEncontradoException(id));
        if (requestDTO.getNome() == null || UniqueComparisonNormalizer.equalsCatalogKey(canal.getNome(), requestDTO.getNome())) {
            throw AtualizacaoSemAlteracoesException.para(
                    "canalVenda",
                    id,
                    "Nenhuma alteração foi informada para atualizar o canal de venda."
            );
        }
        validarNomeDisponivelParaAtualizacao(id, requestDTO.getNome());
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
        CanalVenda canal = canalVendaRepository.findById(id)
                .orElseThrow(() -> new CanalVendaNaoEncontradoException(id));

        List<Estoque> estoques = estoqueRepository.findAllByCanalVenda(canal);
        List<Venda> vendas = vendaRepository.findAllByCanalVenda(canal);
        List<OrdemDeProducao> ordens = ordemDeProducaoRepository.findAllByCanalVendaDestinoId(id);

        if (!estoques.isEmpty() || !vendas.isEmpty() || !ordens.isEmpty()) {
            throw new CanalVendaEmUsoException(
                    id,
                    estoques.stream().map(Estoque::getId).collect(Collectors.toSet()),
                    vendas.stream().map(Venda::getId).collect(Collectors.toSet()),
                    ordens.stream().map(OrdemDeProducao::getId).collect(Collectors.toSet())
            );
        }

        canalVendaRepository.delete(canal);
    }

    private void validarNomeDisponivelParaCriacao(String nome) {
        String normalizedNome = HumanTextNormalizer.normalize(nome);
        if (normalizedNome != null && normalizedUniquenessChecker.existsCanalVendaNome(normalizedNome)) {
            throw new CanalVendaNomeDuplicadoException(normalizedNome);
        }
    }

    private void validarNomeDisponivelParaAtualizacao(Long id, String nome) {
        String normalizedNome = HumanTextNormalizer.normalize(nome);
        if (normalizedNome != null && normalizedUniquenessChecker.existsCanalVendaNome(id, normalizedNome)) {
            throw new CanalVendaNomeDuplicadoException(normalizedNome);
        }
    }
}
