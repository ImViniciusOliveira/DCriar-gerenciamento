package com.dcriar.domain.stock.service.impl;

import com.dcriar.api.dto.request.stock.TipoMateriaPrimaRequestDTO;
import com.dcriar.api.dto.response.stock.TipoMateriaPrimaResponseDTO;
import com.dcriar.api.mapper.stock.TipoMateriaPrimaMapper;
import com.dcriar.domain.stock.entity.LoteMateriaPrima;
import com.dcriar.domain.stock.entity.TipoMateriaPrima;
import com.dcriar.domain.stock.entity.enums.UnidadeDeMedida;
import com.dcriar.domain.stock.repository.LoteMateriaPrimaRepository;
import com.dcriar.domain.stock.repository.TipoMateriaPrimaRepository;
import com.dcriar.domain.stock.repository.TipoMateriaPrimaSpecification;
import com.dcriar.domain.stock.service.TipoMateriaPrimaService;
import com.dcriar.exception.custom.TipoMateriaPrimaJaExisteException;
import com.dcriar.exception.custom.TipoMateriaPrimaEmUsoException;
import com.dcriar.exception.custom.TipoMateriaPrimaNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementação da lógica de negócio para o gerenciamento de Tipos de Matéria-Prima.
 * <p>
 * Esta classe é responsável por todas as operações de CRUD e regras de negócio
 * relacionadas aos tipos de matéria-prima, como a criação, atualização, busca
 * e exclusão, garantindo a consistência dos dados.
 */
@Service
@RequiredArgsConstructor
public class TipoMateriaPrimaServiceImpl implements TipoMateriaPrimaService {

    private final TipoMateriaPrimaRepository tipoMateriaPrimaRepository;
    private final TipoMateriaPrimaMapper tipoMateriaPrimaMapper;
    private final LoteMateriaPrimaRepository loteMateriaPrimaRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<TipoMateriaPrimaResponseDTO> findAll(String nome, UnidadeDeMedida unidadeDeConsumo, Pageable pageable) {
        Specification<TipoMateriaPrima> spec = Stream.of(
                TipoMateriaPrimaSpecification.comNomeSemelhante(nome),
                TipoMateriaPrimaSpecification.comUnidadeDeConsumo(unidadeDeConsumo)
        )
        .filter(Objects::nonNull)
        .reduce(Specification::and)
        .orElse(null);

        Page<TipoMateriaPrima> paginaDeEntidades = tipoMateriaPrimaRepository.findAll(spec, pageable);

        return paginaDeEntidades.map(tipoMateriaPrimaMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public TipoMateriaPrimaResponseDTO findById(Long id) {
        TipoMateriaPrima tipo = findTipoById(id);
        return tipoMateriaPrimaMapper.toResponseDTO(tipo);
    }

    @Override
    @Transactional
    public TipoMateriaPrimaResponseDTO create(TipoMateriaPrimaRequestDTO requestDTO) {
        validateNomeDisponivel(requestDTO.getNome());
        
        // Usa o mapper para a conversão, centralizando a lógica de mapeamento
        TipoMateriaPrima tipo = tipoMateriaPrimaMapper.toEntity(requestDTO);
        
        TipoMateriaPrima salvo = tipoMateriaPrimaRepository.save(tipo);
        return tipoMateriaPrimaMapper.toResponseDTO(salvo);
    }

    @Override
    @Transactional
    public TipoMateriaPrimaResponseDTO update(Long id, TipoMateriaPrimaRequestDTO requestDTO) {
        TipoMateriaPrima tipo = findTipoById(id);
        if (requestDTO.getNome() != null && !tipo.getNome().equalsIgnoreCase(requestDTO.getNome())) {
            validateNomeDisponivel(requestDTO.getNome());
        }
        
        // A lógica de atualização permanece na entidade por enquanto,
        // mas a criação agora usa o mapper.
        tipo.updateFrom(requestDTO); 

        TipoMateriaPrima atualizado = tipoMateriaPrimaRepository.save(tipo);
        return tipoMateriaPrimaMapper.toResponseDTO(atualizado);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        TipoMateriaPrima tipo = findTipoById(id);

        List<LoteMateriaPrima> lotes = loteMateriaPrimaRepository.findAllByTipoMateriaPrima(tipo);
        if (!lotes.isEmpty()) {
            Set<Long> loteIds = lotes.stream().map(LoteMateriaPrima::getId).collect(Collectors.toSet());
            throw new TipoMateriaPrimaEmUsoException(id, loteIds);
        }

        tipoMateriaPrimaRepository.delete(tipo);
    }

    private TipoMateriaPrima findTipoById(Long id) {
        return tipoMateriaPrimaRepository.findById(id)
                .orElseThrow(() -> new TipoMateriaPrimaNaoEncontradoException(id));
    }

    private void validateNomeDisponivel(String nome) {
        if (tipoMateriaPrimaRepository.existsByNome(nome)) {
            throw new TipoMateriaPrimaJaExisteException(nome);
        }
    }
}
