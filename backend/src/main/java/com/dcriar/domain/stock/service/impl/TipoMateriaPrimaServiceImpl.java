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

    /**
     * Retorna uma lista paginada de todos os tipos de matéria-prima, com possibilidade de filtro.
     *
     * @param nome              Filtro para buscar tipos de matéria-prima por nome (busca parcial, case-insensitive).
     * @param unidadeDeConsumo  Filtro para buscar tipos de matéria-prima por unidade de consumo.
     * @param pageable          Informações de paginação e ordenação.
     * @return Uma página ({@link Page}) de {@link TipoMateriaPrimaResponseDTO}.
     */
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

    /**
     * Busca um tipo de matéria-prima específico pelo seu ID.
     *
     * @param id O ID do tipo de matéria-prima a ser buscado.
     * @return O {@link TipoMateriaPrimaResponseDTO} correspondente ao ID.
     * @throws TipoMateriaPrimaNaoEncontradoException se o tipo de matéria-prima com o ID especificado não for encontrado.
     */
    @Override
    @Transactional(readOnly = true)
    public TipoMateriaPrimaResponseDTO findById(Long id) {
        TipoMateriaPrima tipo = findTipoById(id);
        return tipoMateriaPrimaMapper.toResponseDTO(tipo);
    }

    /**
     * Cria um novo tipo de matéria-prima no sistema.
     * <p>
     * Antes de criar, verifica se já existe um tipo de matéria-prima com o mesmo nome.
     * A criação da entidade é feita pelo método de fábrica {@link TipoMateriaPrima#from(TipoMateriaPrimaRequestDTO)},
     * que centraliza regras de negócio como normalização e validação dos campos.
     *
     * @param requestDTO O DTO com os dados para a criação do tipo de matéria-prima.
     * @return O {@link TipoMateriaPrimaResponseDTO} do tipo recém-criado.
     * @throws TipoMateriaPrimaJaExisteException se já existir um tipo de matéria-prima com o nome fornecido.
     */
    @Override
    @Transactional
    public TipoMateriaPrimaResponseDTO create(TipoMateriaPrimaRequestDTO requestDTO) {
        validateNomeDisponivel(requestDTO.getNome());
        TipoMateriaPrima tipo = TipoMateriaPrima.from(requestDTO);
        TipoMateriaPrima salvo = tipoMateriaPrimaRepository.save(tipo);
        return tipoMateriaPrimaMapper.toResponseDTO(salvo);
    }

    /**
     * Atualiza um tipo de matéria-prima existente pelo seu ID.
     * <p>
     * Permite a atualização do nome e da unidade de consumo. Se o nome for alterado,
     * verifica se o novo nome já não está em uso por outro tipo de matéria-prima.
     * A atualização dos campos é feita pelo método {@link TipoMateriaPrima#updateFrom(TipoMateriaPrimaRequestDTO)},
     * que centraliza regras de negócio como normalização e validação dos campos.
     *
     * @param id O ID do tipo de matéria-prima a ser atualizado.
     * @param requestDTO O DTO com os novos dados.
     * @return O {@link TipoMateriaPrimaResponseDTO} do tipo atualizado.
     * @throws TipoMateriaPrimaNaoEncontradoException se o tipo de matéria-prima com o ID especificado não for encontrado.
     * @throws TipoMateriaPrimaJaExisteException se o novo nome fornecido já estiver em uso por outro tipo.
     */
    @Override
    @Transactional
    public TipoMateriaPrimaResponseDTO update(Long id, TipoMateriaPrimaRequestDTO requestDTO) {
        TipoMateriaPrima tipo = findTipoById(id);
        if (requestDTO.getNome() != null && !tipo.getNome().equalsIgnoreCase(requestDTO.getNome())) {
            validateNomeDisponivel(requestDTO.getNome());
        }
        tipo.updateFrom(requestDTO);
        TipoMateriaPrima atualizado = tipoMateriaPrimaRepository.save(tipo);
        return tipoMateriaPrimaMapper.toResponseDTO(atualizado);
    }

    /**
     * Deleta um tipo de matéria-prima pelo seu ID.
     * <p>
     * Antes de deletar, verifica se o tipo de matéria-prima não está em uso por nenhum lote.
     *
     * @param id O ID do tipo de matéria-prima a ser deletado.
     * @throws TipoMateriaPrimaNaoEncontradoException se o tipo de matéria-prima com o ID especificado não for encontrado.
     * @throws TipoMateriaPrimaEmUsoException se o tipo de matéria-prima estiver em uso por um ou mais lotes.
     */
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

    /**
     * Busca uma entidade {@link TipoMateriaPrima} pelo seu ID.
     * Método auxiliar para evitar duplicação de código e centralizar o tratamento de "não encontrado".
     *
     * @param id O ID do tipo de matéria-prima a ser buscado.
     * @return A entidade {@link TipoMateriaPrima} encontrada.
     * @throws TipoMateriaPrimaNaoEncontradoException se o tipo de matéria-prima com o ID especificado não for encontrado.
     */
    private TipoMateriaPrima findTipoById(Long id) {
        return tipoMateriaPrimaRepository.findById(id)
                .orElseThrow(() -> new TipoMateriaPrimaNaoEncontradoException(id));
    }

    /**
     * Valida se um nome de tipo de matéria-prima já está em uso.
     * Método auxiliar para evitar duplicação de código.
     *
     * @param nome O nome a ser validado.
     * @throws TipoMateriaPrimaJaExisteException se já existir um tipo de matéria-prima com o nome fornecido.
     */
    private void validateNomeDisponivel(String nome) {
        if (tipoMateriaPrimaRepository.existsByNome(nome)) {
            throw new TipoMateriaPrimaJaExisteException(nome);
        }
    }
}
