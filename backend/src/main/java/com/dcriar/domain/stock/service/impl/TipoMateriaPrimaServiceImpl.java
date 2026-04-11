package com.dcriar.domain.stock.service.impl;

import com.dcriar.api.dto.request.stock.TipoMateriaPrimaRequestDTO;
import com.dcriar.api.dto.response.stock.TipoMateriaPrimaResponseDTO;
import com.dcriar.api.mapper.stock.TipoMateriaPrimaMapper;
import com.dcriar.domain.common.model.CamposBloqueadosInfo;
import com.dcriar.domain.common.persistence.NormalizedUniquenessChecker;
import com.dcriar.domain.common.util.CamposBloqueadosUtils;
import com.dcriar.domain.common.util.HumanTextNormalizer;
import com.dcriar.domain.common.util.PageableSortUtils;
import com.dcriar.domain.common.util.UniqueComparisonNormalizer;
import com.dcriar.domain.product.entity.Produto;
import com.dcriar.domain.product.repository.ProdutoRepository;
import com.dcriar.domain.stock.entity.LoteMateriaPrima;
import com.dcriar.domain.stock.entity.TipoMateriaPrima;
import com.dcriar.domain.stock.entity.enums.UnidadeDeMedida;
import com.dcriar.domain.stock.repository.LoteMateriaPrimaRepository;
import com.dcriar.domain.stock.repository.TipoMateriaPrimaRepository;
import com.dcriar.domain.stock.util.LotePublicIdentifierFormatter;
import com.dcriar.domain.stock.repository.TipoMateriaPrimaSpecification;
import com.dcriar.domain.stock.service.TipoMateriaPrimaService;
import com.dcriar.exception.custom.TipoMateriaPrimaJaExisteException;
import com.dcriar.exception.custom.TipoMateriaPrimaCamposBloqueadosException;
import com.dcriar.exception.custom.TipoMateriaPrimaEmUsoException;
import com.dcriar.exception.custom.TipoMateriaPrimaNaoEncontradoException;
import com.dcriar.exception.custom.TipoProdutoInvalidoException;
import com.dcriar.exception.custom.AtualizacaoSemAlteracoesException;
import com.dcriar.exception.custom.FaixaEstoqueMateriaPrimaInvalidaException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
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
    private static final Map<String, String> STABLE_SORTS = Map.of("nome", "id");

    private final TipoMateriaPrimaRepository tipoMateriaPrimaRepository;
    private final TipoMateriaPrimaMapper tipoMateriaPrimaMapper;
    private final NormalizedUniquenessChecker normalizedUniquenessChecker;
    private final LoteMateriaPrimaRepository loteMateriaPrimaRepository;
    private final ProdutoRepository produtoRepository;

    private static final Set<String> CAMPOS_SENSIVEIS_TIPO_MATERIA_PRIMA = Set.of("unidadeDeConsumo");
    private static final String MOTIVO_BLOQUEIO_TIPO_MATERIA_PRIMA_EM_USO =
            "Tipo de matéria-prima já utilizado por produtos ou lotes.";

    @Override
    @Transactional(readOnly = true)
    public Page<TipoMateriaPrimaResponseDTO> findAll(String nome, UnidadeDeMedida unidadeDeConsumo, String tipoProduto, Pageable pageable) {
        validarTipoProduto(tipoProduto);

        Specification<TipoMateriaPrima> spec = Stream.of(
                TipoMateriaPrimaSpecification.comNomeSemelhante(nome),
                TipoMateriaPrimaSpecification.comUnidadeDeConsumo(unidadeDeConsumo),
                TipoMateriaPrimaSpecification.compativelComTipoProduto(tipoProduto)
        )
        .filter(Objects::nonNull)
        .reduce(Specification::and)
        .orElse(null);

        Pageable pageableComDesempate = PageableSortUtils.withStableSort(pageable, STABLE_SORTS);
        Page<TipoMateriaPrima> paginaDeEntidades = tipoMateriaPrimaRepository.findAll(spec, pageableComDesempate);

        return paginaDeEntidades.map(this::mapAndEnrichTipo);
    }

    @Override
    @Transactional(readOnly = true)
    public TipoMateriaPrimaResponseDTO findById(Long id) {
        TipoMateriaPrima tipo = findTipoById(id);
        return mapAndEnrichTipo(tipo);
    }

    @Override
    @Transactional
    public TipoMateriaPrimaResponseDTO create(TipoMateriaPrimaRequestDTO requestDTO) {
        validateNomeDisponivel(requestDTO.getNome());
        validarFaixaEstoque(requestDTO.getEstoqueCritico(), requestDTO.getEstoqueAceitavel());
        
        // Usa o mapper para a conversão, centralizando a lógica de mapeamento
        TipoMateriaPrima tipo = tipoMateriaPrimaMapper.toEntity(requestDTO);
        
        TipoMateriaPrima salvo = tipoMateriaPrimaRepository.save(tipo);
        return mapAndEnrichTipo(salvo);
    }

    @Override
    @Transactional
    public TipoMateriaPrimaResponseDTO update(Long id, TipoMateriaPrimaRequestDTO requestDTO) {
        TipoMateriaPrima tipo = findTipoById(id);
        validarFaixaEstoque(
                requestDTO.getEstoqueCritico() != null ? requestDTO.getEstoqueCritico() : tipo.getEstoqueCritico(),
                requestDTO.getEstoqueAceitavel() != null ? requestDTO.getEstoqueAceitavel() : tipo.getEstoqueAceitavel()
        );
        if (isNoOpUpdate(tipo, requestDTO)) {
            throw AtualizacaoSemAlteracoesException.para(
                    "tipoMateriaPrima",
                    id,
                    "Nenhuma alteração foi informada para atualizar o tipo de matéria-prima."
            );
        }
        validarCamposBloqueadosNaEdicao(tipo, requestDTO);
        if (requestDTO.getNome() != null
                && !UniqueComparisonNormalizer.equalsCatalogKey(tipo.getNome(), requestDTO.getNome())) {
            validateNomeDisponivel(requestDTO.getNome());
        }
        
        // A lógica de atualização permanece na entidade por enquanto,
        // mas a criação agora usa o mapper.
        tipo.updateFrom(requestDTO); 

        TipoMateriaPrima atualizado = tipoMateriaPrimaRepository.save(tipo);
        return mapAndEnrichTipo(atualizado);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        TipoMateriaPrima tipo = findTipoById(id);

        List<Produto> produtos = produtoRepository.findAllByTipoMateriaPrima(tipo);
        List<LoteMateriaPrima> lotes = loteMateriaPrimaRepository.findAllByTipoMateriaPrima(tipo);
        if (!produtos.isEmpty() || !lotes.isEmpty()) {
            Set<Long> produtoIds = produtos.stream().map(Produto::getId).collect(Collectors.toSet());
            Set<String> produtoLabels = produtos.stream()
                    .map(this::formatarProdutoLabel)
                    .collect(Collectors.toSet());
            Set<Long> loteIds = lotes.stream().map(LoteMateriaPrima::getId).collect(Collectors.toSet());
            Set<String> loteLabels = lotes.stream()
                    .map(LotePublicIdentifierFormatter::format)
                    .collect(Collectors.toSet());
            throw new TipoMateriaPrimaEmUsoException(id, tipo.getNome(), produtoIds, produtoLabels, loteIds, loteLabels);
        }

        tipoMateriaPrimaRepository.delete(tipo);
    }

    private TipoMateriaPrima findTipoById(Long id) {
        return tipoMateriaPrimaRepository.findById(id)
                .orElseThrow(() -> new TipoMateriaPrimaNaoEncontradoException(id));
    }

    private TipoMateriaPrimaResponseDTO mapAndEnrichTipo(TipoMateriaPrima tipo) {
        TipoMateriaPrimaResponseDTO dto = tipoMateriaPrimaMapper.toResponseDTO(tipo);
        CamposBloqueadosInfo camposBloqueados = resolverCamposBloqueados(tipo);
        dto.setCamposBloqueados(camposBloqueados.camposBloqueados());
        dto.setMotivosBloqueio(camposBloqueados.motivosBloqueio());
        return dto;
    }

    private void validateNomeDisponivel(String nome) {
        String normalizedNome = HumanTextNormalizer.normalize(nome);
        if (normalizedNome != null && normalizedUniquenessChecker.existsTipoMateriaPrimaNome(normalizedNome)) {
            throw new TipoMateriaPrimaJaExisteException(normalizedNome);
        }
    }

    private void validarTipoProduto(String tipoProduto) {
        if (tipoProduto == null || tipoProduto.isBlank()) {
            return;
        }

        if (!"CORTE".equalsIgnoreCase(tipoProduto) && !"CONSUMO".equalsIgnoreCase(tipoProduto)) {
            throw new TipoProdutoInvalidoException(tipoProduto);
        }
    }

    private void validarCamposBloqueadosNaEdicao(TipoMateriaPrima tipo, TipoMateriaPrimaRequestDTO requestDTO) {
        CamposBloqueadosInfo camposBloqueados = resolverCamposBloqueados(tipo);
        Set<String> tentativaCamposSensveis = CamposBloqueadosUtils.intersectarTentativas(
                camposBloqueados,
                Stream.of(requestDTO.getUnidadeDeConsumo() != null ? "unidadeDeConsumo" : null)
                        .filter(Objects::nonNull)
        );

        if (!tentativaCamposSensveis.isEmpty()) {
            throw new TipoMateriaPrimaCamposBloqueadosException(
                    tipo.getId(),
                    tipo.getNome(),
                    tentativaCamposSensveis,
                    camposBloqueados.motivosBloqueio()
            );
        }
    }

    private CamposBloqueadosInfo resolverCamposBloqueados(TipoMateriaPrima tipo) {
        if (!tipoMateriaPrimaPossuiUsoOperacional(tipo)) {
            return CamposBloqueadosInfo.vazio();
        }

        return CamposBloqueadosUtils.bloquearTodos(
                CAMPOS_SENSIVEIS_TIPO_MATERIA_PRIMA,
                MOTIVO_BLOQUEIO_TIPO_MATERIA_PRIMA_EM_USO
        );
    }

    private boolean tipoMateriaPrimaPossuiUsoOperacional(TipoMateriaPrima tipo) {
        return !produtoRepository.findAllByTipoMateriaPrima(tipo).isEmpty()
                || !loteMateriaPrimaRepository.findAllByTipoMateriaPrima(tipo).isEmpty();
    }

    private String formatarProdutoLabel(Produto produto) {
        return produto.getSku() + " - " + produto.getNome();
    }

    private boolean isNoOpUpdate(TipoMateriaPrima tipo, TipoMateriaPrimaRequestDTO requestDTO) {
        boolean sameNome = requestDTO.getNome() == null
                || UniqueComparisonNormalizer.equalsCatalogKey(tipo.getNome(), requestDTO.getNome());
        boolean sameUnidade = requestDTO.getUnidadeDeConsumo() == null
                || Objects.equals(tipo.getUnidadeDeConsumo(), requestDTO.getUnidadeDeConsumo());
        boolean sameEstoqueCritico = sameBigDecimal(tipo.getEstoqueCritico(), requestDTO.getEstoqueCritico());
        boolean sameEstoqueAceitavel = sameBigDecimal(tipo.getEstoqueAceitavel(), requestDTO.getEstoqueAceitavel());
        return sameNome && sameUnidade && sameEstoqueCritico && sameEstoqueAceitavel;
    }

    private boolean sameBigDecimal(BigDecimal atual, BigDecimal informado) {
        return informado == null || (atual != null && atual.compareTo(informado) == 0);
    }

    private void validarFaixaEstoque(BigDecimal estoqueCritico, BigDecimal estoqueAceitavel) {
        if (estoqueCritico == null && estoqueAceitavel == null) {
            return;
        }
        if (estoqueCritico == null || estoqueAceitavel == null) {
            throw FaixaEstoqueMateriaPrimaInvalidaException.parametrosDevemSerInformadosEmConjunto(
                    estoqueCritico,
                    estoqueAceitavel
            );
        }
        if (estoqueAceitavel.compareTo(estoqueCritico) <= 0) {
            throw FaixaEstoqueMateriaPrimaInvalidaException.aceitavelDeveSerMaiorQueCritico(
                    estoqueCritico,
                    estoqueAceitavel
            );
        }
    }
}
