package com.dcriar.domain.product.service.impl;

import com.dcriar.api.dto.request.product.ProdutoRequestDTO;
import com.dcriar.api.dto.response.product.ProdutoResponseDTO;
import com.dcriar.api.mapper.product.ProdutoMapper;
import com.dcriar.domain.product.entity.*;
import com.dcriar.domain.product.repository.EstoqueRepository;
import com.dcriar.domain.product.repository.MovimentacaoEstoqueProdutoRepository;
import com.dcriar.domain.product.repository.ProdutoDeConsumoRepository;
import com.dcriar.domain.product.repository.ProdutoRepository;
import com.dcriar.domain.product.repository.spec.ProdutoSpecifications;
import com.dcriar.domain.production.entity.OrdemDeProducao;
import com.dcriar.domain.production.repository.OrdemDeProducaoRepository;
import com.dcriar.domain.stock.entity.TipoMateriaPrima;
import com.dcriar.domain.stock.entity.enums.UnidadeDeMedida;
import com.dcriar.domain.stock.repository.TipoMateriaPrimaRepository;
import com.dcriar.domain.product.service.ProdutoService;
import com.dcriar.domain.upload.service.FileStorageService;
import com.dcriar.exception.custom.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProdutoServiceImpl implements ProdutoService {

    private final ProdutoRepository produtoRepository;
    private final ProdutoDeConsumoRepository produtoDeConsumoRepository;
    private final TipoMateriaPrimaRepository tipoMateriaPrimaRepository;
    private final MovimentacaoEstoqueProdutoRepository movimentacaoEstoqueProdutoRepository;
    private final EstoqueRepository estoqueRepository;
    private final OrdemDeProducaoRepository ordemDeProducaoRepository;
    private final ProdutoMapper produtoMapper;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional(readOnly = true)
    public Page<ProdutoResponseDTO> findAll(String nome, Pageable pageable) {
        Page<Produto> produtoPage;
        if (nome != null && !nome.isBlank()) {
            produtoPage = produtoRepository.findByNomeContainingIgnoreCaseOrSkuContainingIgnoreCase(nome, nome, pageable);
        } else {
            produtoPage = produtoRepository.findAll(pageable);
        }
        return produtoPage.map(this::mapAndEnrichProduto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProdutoResponseDTO> findByTipoAndEstoque(
            String tipoProduto,
            Integer estoqueValor,
            String estoqueOperador,
            String nome,
            Pageable pageable) {

        // Valida o operador de estoque para segurança.
        if (estoqueOperador != null && (!estoqueOperador.equalsIgnoreCase("GTE") && !estoqueOperador.equalsIgnoreCase("LTE"))) {
            throw new OperadorEstoqueInvalidoException(estoqueOperador);
        }
        if (tipoProduto != null
                && !tipoProduto.isBlank()
                && !tipoProduto.equalsIgnoreCase("CORTE")
                && !tipoProduto.equalsIgnoreCase("CONSUMO")) {
            throw new TipoProdutoInvalidoException(tipoProduto);
        }

        Specification<Produto> spec = (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();

        if (nome != null && !nome.isBlank()) {
            spec = spec.and(ProdutoSpecifications.comNomeLike(nome));
        }
        if (tipoProduto != null && !tipoProduto.isBlank()) {
            spec = spec.and(ProdutoSpecifications.comTipo(tipoProduto));
        }
        if (estoqueValor != null && estoqueOperador != null && !estoqueOperador.isBlank()) {
            spec = spec.and(ProdutoSpecifications.comEstoque(estoqueValor, estoqueOperador));
        }

        // Executa a busca no banco de dados com os filtros aplicados.
        Page<Produto> produtoPage = produtoRepository.findAll(spec, pageable);

        // Mapeia e enriquece a página de resultados para o DTO de resposta.
        return produtoPage.map(this::mapAndEnrichProduto);
    }

    @Override
    @Transactional(readOnly = true)
    public ProdutoResponseDTO findById(Long id) {
        Produto produto = findProdutoById(id);
        return mapAndEnrichProduto(produto);
    }

    @Override
    @Transactional
    public ProdutoResponseDTO create(ProdutoRequestDTO requestDTO) {
        validarRegrasDeNegocio(requestDTO);

        TipoMateriaPrima tipoMateriaPrima = tipoMateriaPrimaRepository.findById(requestDTO.getTipoMateriaPrimaId())
                .orElseThrow(() -> new TipoMateriaPrimaNaoEncontradoException(requestDTO.getTipoMateriaPrimaId()));

        Produto produto;
        if ("CORTE".equalsIgnoreCase(requestDTO.getTipoProduto())) {
            produto = createProdutoDeCorte(requestDTO, tipoMateriaPrima);
            produto = produtoRepository.save(produto);
        } else if ("CONSUMO".equalsIgnoreCase(requestDTO.getTipoProduto())) {
            produto = createProdutoDeConsumo(requestDTO, tipoMateriaPrima);
            produto = produtoDeConsumoRepository.save((ProdutoDeConsumo) produto);
        } else {
            throw new TipoProdutoInvalidoException(requestDTO.getTipoProduto());
        }

        if (produto.getFotoPrincipalUrl() != null && !produto.getFotoPrincipalUrl().isBlank()) {
            String fileName = fileStorageService.extractFileName(produto.getFotoPrincipalUrl());
            produto.setFotoPrincipalUrl(fileName);
        }

        Produto produtoSalvo = produtoRepository.save(produto);
        return findById(produtoSalvo.getId());
    }

    private ProdutoDeCorte createProdutoDeCorte(ProdutoRequestDTO requestDTO, TipoMateriaPrima tipoMateriaPrima) {
        Dimensoes dimensoes = Dimensoes.builder()
                .larguraCm(requestDTO.getDimensoes().getLarguraCm())
                .comprimentoCm(requestDTO.getDimensoes().getComprimentoCm())
                .build();

        return ProdutoDeCorte.builder()
                .nome(requestDTO.getNome())
                .sku(requestDTO.getSku())
                .descricao(requestDTO.getDescricao())
                .unidadesPorProduto(requestDTO.getUnidadesPorProduto())
                .fotoPrincipalUrl(requestDTO.getFotoPrincipalUrl())
                .ativo(requestDTO.getAtivo() != null ? requestDTO.getAtivo() : true)
                .tipoMateriaPrima(tipoMateriaPrima)
                .cor(requestDTO.getCor())
                .dimensoes(dimensoes)
                .build();
    }

    private ProdutoDeConsumo createProdutoDeConsumo(ProdutoRequestDTO requestDTO, TipoMateriaPrima tipoMateriaPrima) {
        UnidadeDeMedida unidadeCadastro = resolverUnidadeCadastroConsumo(
                tipoMateriaPrima.getUnidadeDeConsumo(),
                requestDTO.getUnidadeCadastroConsumo()
        );
        return ProdutoDeConsumo.builder()
                .nome(requestDTO.getNome())
                .sku(requestDTO.getSku())
                .descricao(requestDTO.getDescricao())
                .unidadesPorProduto(normalizarUnidadesPorProdutoConsumo(
                        requestDTO.getUnidadesPorProduto(),
                        tipoMateriaPrima.getUnidadeDeConsumo(),
                        unidadeCadastro
                ))
                .fotoPrincipalUrl(requestDTO.getFotoPrincipalUrl())
                .ativo(requestDTO.getAtivo() != null ? requestDTO.getAtivo() : true)
                .tipoMateriaPrima(tipoMateriaPrima)
                .codigoFabricante(requestDTO.getCodigoFabricante())
                .unidadeCadastroConsumo(unidadeCadastro)
                .especificacoes(requestDTO.getEspecificacoes())
                .build();
    }

    @Override
    @Transactional
    public ProdutoResponseDTO update(Long id, ProdutoRequestDTO requestDTO) {
        // A lógica de update precisaria ser completamente reescrita para lidar com a herança.
        // Por simplicidade, vamos assumir que o PATCH é o método preferencial para atualizações.
        // Implementar um PUT completo exigiria uma lógica complexa para converter entre tipos de produto,
        // o que geralmente não é uma operação de negócio desejável.
        throw new UnsupportedOperationException("A atualização completa (PUT) de produtos não é suportada. Utilize o PATCH.");
    }

    @Override
    @Transactional
    public ProdutoResponseDTO patch(Long id, Map<String, Object> fields) {
        if (fields == null || fields.isEmpty()) {
            return findById(id);
        }

        Produto produto = findProdutoById(id);

        // Mapeamento Manual de Alta Performance
        fields.forEach((key, value) -> {
            switch (key) {
                case "nome" -> produto.setNome((String) value);
                case "sku" -> produto.setSku((String) value);
                case "descricao" -> produto.setDescricao((String) value);
                case "ativo" -> produto.setAtivo((Boolean) value);
                case "fotoPrincipalUrl" -> {
                    // Lógica para exclusão segura da foto
                    String oldFoto = produto.getFotoPrincipalUrl();
                    String newFoto = (String) value;

                    // Se o novo valor for nulo e existia uma foto antiga, exclui o arquivo físico
                    if (newFoto == null && oldFoto != null && !oldFoto.isBlank()) {
                        fileStorageService.deleteFile(oldFoto);
                    }
                    produto.setFotoPrincipalUrl(newFoto);
                }
                case "unidadesPorProduto" -> {
                    if (value != null) {
                        produto.setUnidadesPorProduto(new BigDecimal(value.toString()));
                    }
                }
                // Campos específicos de ProdutoDeCorte
                case "cor" -> {
                    if (produto instanceof ProdutoDeCorte p) p.setCor((String) value);
                }
                case "dimensoes" -> {
                    if (produto instanceof ProdutoDeCorte p && value instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> dimensoesMap = (Map<String, Object>) value;
                        Dimensoes.DimensoesBuilder builder = p.getDimensoes() != null ?
                                p.getDimensoes().toBuilder() : Dimensoes.builder();

                        if (dimensoesMap.containsKey("larguraCm")) {
                            builder.larguraCm(new BigDecimal(dimensoesMap.get("larguraCm").toString()));
                        }
                        if (dimensoesMap.containsKey("comprimentoCm")) {
                            builder.comprimentoCm(new BigDecimal(dimensoesMap.get("comprimentoCm").toString()));
                        }
                        p.setDimensoes(builder.build());
                    }
                }
                // Campos específicos de ProdutoDeConsumo
                case "codigoFabricante" -> {
                    if (produto instanceof ProdutoDeConsumo p) p.setCodigoFabricante((String) value);
                }
                case "unidadeCadastroConsumo" -> {
                    if (produto instanceof ProdutoDeConsumo p && value != null) {
                        p.setUnidadeCadastroConsumo(UnidadeDeMedida.valueOf(value.toString()));
                    }
                }
                case "especificacoes" -> {
                    if (produto instanceof ProdutoDeConsumo p && value instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, String> incomingSpecs = (Map<String, String>) value;

                        if (p.getEspecificacoes() == null) {
                            p.setEspecificacoes(new HashMap<>());
                        }

                        incomingSpecs.forEach((specKey, specValue) -> {
                            if (specValue == null) {
                                p.getEspecificacoes().remove(specKey);
                            } else {
                                p.getEspecificacoes().put(specKey, specValue);
                            }
                        });
                    }
                }
            }
        });

        if (fields.containsKey("tipoMateriaPrimaId")) {
            Long tipoMateriaPrimaId = ((Number) fields.get("tipoMateriaPrimaId")).longValue();
            TipoMateriaPrima tipoMateriaPrima = tipoMateriaPrimaRepository.findById(tipoMateriaPrimaId)
                    .orElseThrow(() -> new TipoMateriaPrimaNaoEncontradoException(tipoMateriaPrimaId));
            produto.setTipoMateriaPrima(tipoMateriaPrima);
        }

        if (produto instanceof ProdutoDeConsumo produtoDeConsumo &&
                (fields.containsKey("unidadesPorProduto") || fields.containsKey("unidadeCadastroConsumo") || fields.containsKey("tipoMateriaPrimaId"))) {
            UnidadeDeMedida unidadeInformada = fields.containsKey("unidadeCadastroConsumo")
                    ? UnidadeDeMedida.valueOf(fields.get("unidadeCadastroConsumo").toString())
                    : produtoDeConsumo.getUnidadeCadastroConsumo();
            UnidadeDeMedida unidadeCadastro = resolverUnidadeCadastroConsumo(
                    produtoDeConsumo.getTipoMateriaPrima().getUnidadeDeConsumo(),
                    unidadeInformada
            );

            produtoDeConsumo.setUnidadesPorProduto(normalizarUnidadesPorProdutoConsumo(
                    produtoDeConsumo.getUnidadesPorProduto(),
                    produtoDeConsumo.getTipoMateriaPrima().getUnidadeDeConsumo(),
                    unidadeCadastro
            ));
            produtoDeConsumo.setUnidadeCadastroConsumo(unidadeCadastro);
        }

        Produto produtoAtualizado = produtoRepository.save(produto);
        return mapAndEnrichProduto(produtoAtualizado);
    }

    private BigDecimal normalizarUnidadesPorProdutoConsumo(
            BigDecimal quantidadeInformada,
            UnidadeDeMedida unidadePrincipal,
            UnidadeDeMedida unidadeInformada
    ) {
        UnidadeDeMedida unidadeEfetiva = resolverUnidadeCadastroConsumo(unidadePrincipal, unidadeInformada);
        if (!unidadePrincipal.aceitaComoCadastroDeConsumo(unidadeEfetiva)) {
            throw UnidadeCadastroConsumoInvalidaException.unidadeIncompativel(unidadePrincipal, unidadeEfetiva);
        }
        return unidadePrincipal.normalizarQuantidadeDeConsumo(quantidadeInformada, unidadeEfetiva);
    }

    private UnidadeDeMedida resolverUnidadeCadastroConsumo(
            UnidadeDeMedida unidadePrincipal,
            UnidadeDeMedida unidadeInformada
    ) {
        return unidadeInformada != null ? unidadeInformada : unidadePrincipal;
    }

    @Override
    @Transactional
    public ProdutoResponseDTO uploadFoto(Long produtoId, MultipartFile file) {
        Produto produto = findProdutoById(produtoId);
        String oldFotoFileName = produto.getFotoPrincipalUrl();
        String newFileName = fileStorageService.storeFile(file);
        produto.setFotoPrincipalUrl(newFileName);
        Produto produtoAtualizado = produtoRepository.save(produto);
        if (oldFotoFileName != null && !oldFotoFileName.isBlank()) {
            fileStorageService.deleteFile(oldFotoFileName);
        }
        return mapAndEnrichProduto(produtoAtualizado);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        Produto produto = findProdutoById(id);
        List<OrdemDeProducao> ordens = ordemDeProducaoRepository.findAllByProduto(produto);
        if (!ordens.isEmpty()) {
            Set<Long> ordemIds = ordens.stream().map(OrdemDeProducao::getId).collect(Collectors.toSet());
            throw new ProdutoEmUsoException(id, ordemIds);
        }
        if (produto.getFotoPrincipalUrl() != null && !produto.getFotoPrincipalUrl().isBlank()) {
            fileStorageService.deleteFile(produto.getFotoPrincipalUrl());
        }
        produtoRepository.delete(produto);
    }

    private ProdutoResponseDTO mapAndEnrichProduto(Produto produto) {
        ProdutoResponseDTO dto = produtoMapper.toResponseDTO(produto);
        Integer estoqueFisicoTotal = movimentacaoEstoqueProdutoRepository.findSaldoByProduto(produto);
        int estoqueDistribuidoTotal = estoqueRepository.findAllByProduto(produto).stream()
                .mapToInt(Estoque::getQuantidade)
                .sum();
        dto.setEstoqueFisicoTotal(estoqueFisicoTotal);
        dto.setEstoqueDistribuidoTotal(estoqueDistribuidoTotal);
        dto.setEstoqueDisponivelParaAlocar(estoqueFisicoTotal - estoqueDistribuidoTotal);
        return dto;
    }

    private Produto findProdutoById(Long id) {
        return produtoRepository.findById(id)
                .orElseThrow(() -> new ProdutoNaoEncontradoException(id));
    }

    private void validarRegrasDeNegocio(ProdutoRequestDTO requestDTO) {
        if (produtoRepository.existsByNome(requestDTO.getNome())) {
            throw new ProdutoNomeDuplicadoException(requestDTO.getNome());
        }
        if (produtoRepository.existsBySku(requestDTO.getSku())) {
            throw new ProdutoSkuDuplicadoException(requestDTO.getSku());
        }
    }
}
