package com.dcriar.domain.product.service.impl;

import com.dcriar.api.dto.request.product.ProdutoRequestDTO;
import com.dcriar.api.dto.response.product.ProdutoResponseDTO;
import com.dcriar.api.mapper.product.ProdutoMapper;
import com.dcriar.domain.product.entity.Dimensoes;
import com.dcriar.domain.product.entity.Estoque;
import com.dcriar.domain.product.entity.Produto;
import com.dcriar.domain.product.repository.EstoqueRepository;
import com.dcriar.domain.product.repository.MovimentacaoEstoqueProdutoRepository;
import com.dcriar.domain.product.repository.ProdutoRepository;
import com.dcriar.domain.production.entity.OrdemDeProducao;
import com.dcriar.domain.production.repository.OrdemDeProducaoRepository;
import com.dcriar.domain.stock.entity.TipoMateriaPrima;
import com.dcriar.domain.stock.repository.TipoMateriaPrimaRepository;
import com.dcriar.domain.product.service.ProdutoService;
import com.dcriar.domain.upload.service.FileStorageService;
import com.dcriar.exception.custom.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementação do serviço de gerenciamento de produtos.
 * <p>
 * Esta classe contém a lógica de negócio para operações de CRUD em produtos,
 * além de orquestrar validações, manipulação de arquivos de imagem e
 * enriquecimento dos dados de resposta com informações de estoque.
 */
@Service
@RequiredArgsConstructor
public class ProdutoServiceImpl implements ProdutoService {

    private final ProdutoRepository produtoRepository;
    private final TipoMateriaPrimaRepository tipoMateriaPrimaRepository;
    private final MovimentacaoEstoqueProdutoRepository movimentacaoEstoqueProdutoRepository;
    private final EstoqueRepository estoqueRepository;
    private final OrdemDeProducaoRepository ordemDeProducaoRepository;
    private final ProdutoMapper produtoMapper;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional(readOnly = true)
    public Page<ProdutoResponseDTO> findAll(Pageable pageable) {
        Page<Produto> produtoPage = produtoRepository.findAll(pageable);
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
        // 1. Valida campos obrigatórios
        Map<String, String> errors = validarCamposObrigatorios(requestDTO);
        if (!errors.isEmpty()) {
            throw new ProdutoInvalidoException("Dados do produto inválidos", errors);
        }
        // 2. Valida regras de negócio de unicidade para nome e SKU.
        validarRegrasDeNegocio(requestDTO, null);

        // 3. Associa o tipo de matéria-prima.
        var tipoMateriaPrima = tipoMateriaPrimaRepository.findById(requestDTO.getTipoMateriaPrimaId())
                .orElseThrow(() -> new TipoMateriaPrimaNaoEncontradoException(requestDTO.getTipoMateriaPrimaId()));

        Produto produto = Produto.from(requestDTO);
        produto.setTipoMateriaPrima(tipoMateriaPrima);

        // 4. Processa a URL da imagem, armazenando apenas o nome do arquivo.
        if (produto.getFotoPrincipalUrl() != null && !produto.getFotoPrincipalUrl().isBlank()) {
            String fileName = fileStorageService.extractFileName(produto.getFotoPrincipalUrl());
            produto.setFotoPrincipalUrl(fileName);
        }

        // 5. Salva o produto no banco de dados.
        Produto produtoSalvo = produtoRepository.save(produto);

        // 6. Retorna o DTO enriquecido.
        return findById(produtoSalvo.getId());
    }

    @Override
    @Transactional
    public ProdutoResponseDTO update(Long id, ProdutoRequestDTO requestDTO) {
        // 1. Valida campos obrigatórios para garantir a integridade do objeto.
        Map<String, String> errors = validarCamposObrigatorios(requestDTO);
        if (!errors.isEmpty()) {
            throw new ProdutoInvalidoException("Dados do produto inválidos", errors);
        }
        // 2. Busca o produto e armazena o nome do arquivo da foto antiga.
        Produto produto = findProdutoById(id);
        String oldFotoFileName = produto.getFotoPrincipalUrl();

        // 3. Valida regras de negócio de unicidade para nome e SKU.
        validarRegrasDeNegocio(requestDTO, id);

        // 4. Associa o tipo de matéria-prima.
        var tipoMateriaPrima = tipoMateriaPrimaRepository.findById(requestDTO.getTipoMateriaPrimaId())
                .orElseThrow(() -> new TipoMateriaPrimaNaoEncontradoException(requestDTO.getTipoMateriaPrimaId()));

        produto.updateFrom(requestDTO);
        produto.setTipoMateriaPrima(tipoMateriaPrima);

        // 5. Gerencia o ciclo de vida do arquivo de imagem.
        String newFotoUrlFromDto = requestDTO.getFotoPrincipalUrl();

        if (newFotoUrlFromDto != null) {
            if (newFotoUrlFromDto.isBlank()) {
                // Intenção explícita de remover a foto.
                if (oldFotoFileName != null && !oldFotoFileName.isBlank()) {
                    fileStorageService.deleteFile(oldFotoFileName);
                }
                produto.setFotoPrincipalUrl(null);
            } else {
                String newFileName;
                // Verifica se a string é uma URL completa ou apenas um nome de arquivo.
                if (newFotoUrlFromDto.startsWith("http://") || newFotoUrlFromDto.startsWith("https://")) {
                    // É uma URL nova, extrai o nome do arquivo.
                    newFileName = fileStorageService.extractFileName(newFotoUrlFromDto);
                } else {
                    // Já é um nome de arquivo (vindo de um PATCH), usa diretamente.
                    newFileName = newFotoUrlFromDto;
                }

                // Se a foto antiga existia e é diferente da nova, exclui a antiga.
                if (oldFotoFileName != null && !oldFotoFileName.equals(newFileName)) {
                    fileStorageService.deleteFile(oldFotoFileName);
                }
                produto.setFotoPrincipalUrl(newFileName);
            }
        }
        // Se newFotoUrlFromDto for nulo, nada acontece, preservando a foto existente.

        // 6. Salva as alterações.
        produtoRepository.save(produto);

        // 7. Retorna o DTO enriquecido.
        return findById(id);
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
                case "cor" -> produto.setCor((String) value);
                case "ativo" -> produto.setAtivo((Boolean) value);
                case "fotoPrincipalUrl" -> produto.setFotoPrincipalUrl((String) value);
                case "unidadesPorProduto" -> {
                    if (value instanceof Number) {
                        produto.setUnidadesPorProduto(((Number) value).intValue());
                    }
                }
                case "dimensoes" -> {
                    if (value instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> dimensoesMap = (Map<String, Object>) value;
                        Dimensoes.DimensoesBuilder builder = produto.getDimensoes() != null ?
                                produto.getDimensoes().toBuilder() : Dimensoes.builder();

                        if (dimensoesMap.containsKey("larguraCm")) {
                            Object largura = dimensoesMap.get("larguraCm");
                            builder.larguraCm(new BigDecimal(largura.toString()));
                        }

                        if (dimensoesMap.containsKey("comprimentoCm")) {
                            Object comprimento = dimensoesMap.get("comprimentoCm");
                            builder.comprimentoCm(new BigDecimal(comprimento.toString()));
                        }
                        produto.setDimensoes(builder.build());
                    }
                }
            }
        });

        // Tratamento específico para o relacionamento com TipoMateriaPrima
        if (fields.containsKey("tipoMateriaPrimaId")) {
            Object mpIdValue = fields.get("tipoMateriaPrimaId");
            if (mpIdValue instanceof Number) {
                Long tipoMateriaPrimaId = ((Number) mpIdValue).longValue();
                TipoMateriaPrima tipoMateriaPrima = tipoMateriaPrimaRepository.findById(tipoMateriaPrimaId)
                        .orElseThrow(() -> new TipoMateriaPrimaNaoEncontradoException(tipoMateriaPrimaId));
                produto.setTipoMateriaPrima(tipoMateriaPrima);
            }
        }

        // Valida as regras de negócio com os dados atualizados
        validarRegrasDeNegocio(produtoMapper.toRequestDTO(produto), id);

        // Salva a entidade atualizada
        Produto produtoAtualizado = produtoRepository.save(produto);

        // Retorna o DTO enriquecido
        return mapAndEnrichProduto(produtoAtualizado);
    }

    @Override
    @Transactional
    public ProdutoResponseDTO uploadFoto(Long produtoId, MultipartFile file) {
        // Passo 1: Busca o produto para garantir que ele existe e para obter o nome do arquivo antigo.
        Produto produto = findProdutoById(produtoId);
        String oldFotoFileName = produto.getFotoPrincipalUrl();

        // Passo 2: Delega o armazenamento do novo arquivo para o serviço de storage.
        String newFileName = fileStorageService.storeFile(file);

        // Passo 3: Associa o nome do novo arquivo à entidade do produto.
        produto.setFotoPrincipalUrl(newFileName);

        // Passo 4: Salva a entidade atualizada no banco de dados.
        Produto produtoAtualizado = produtoRepository.save(produto);

        // Passo 5: Após o sucesso da persistência, exclui o arquivo antigo para evitar inconsistências.
        if (oldFotoFileName != null && !oldFotoFileName.isBlank()) {
            fileStorageService.deleteFile(oldFotoFileName);
        }

        // Passo 6: Retorna o DTO enriquecido com a nova URL completa da foto.
        return mapAndEnrichProduto(produtoAtualizado);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        Produto produto = findProdutoById(id);

        // 1. Verifica se o produto está em uso em Ordens de Produção.
        List<OrdemDeProducao> ordens = ordemDeProducaoRepository.findAllByProduto(produto);
        if (!ordens.isEmpty()) {
            Set<Long> ordemIds = ordens.stream().map(OrdemDeProducao::getId).collect(Collectors.toSet());
            throw new ProdutoEmUsoException(id, ordemIds);
        }

        // 2. Se houver uma foto associada, exclui o arquivo.
        if (produto.getFotoPrincipalUrl() != null && !produto.getFotoPrincipalUrl().isBlank()) {
            fileStorageService.deleteFile(produto.getFotoPrincipalUrl());
        }

        // 3. Exclui o produto do banco de dados.
        produtoRepository.delete(produto);
    }

    /**
     * Mapeia uma entidade {@link Produto} para seu {@link ProdutoResponseDTO} e o enriquece com dados adicionais.
     * <p>
     * Este método realiza as seguintes ações de enriquecimento:
     * <ol>
     *     <li><b>URL da Foto:</b> Constrói a URL de download completa para a foto do produto, se houver.</li>
     *     <li><b>Dados de Estoque:</b> Calcula e adiciona os seguintes campos ao DTO:
     *         <ul>
     *             <li>{@code estoqueFisicoTotal}: O saldo total do produto, com base em todas as movimentações.</li>
     *             <li>{@code estoqueDistribuidoTotal}: A soma das quantidades do produto que estão alocadas nos estoques dos canais de venda.</li>
     *             <li>{@code estoqueDisponivelParaAlocar}: A quantidade do estoque físico que ainda não foi distribuída.</li>
     *         </ul>
     *     </li>
     * </ol>
     *
     * @param produto A entidade {@link Produto} a ser processada.
     * @return O {@link ProdutoResponseDTO} enriquecido.
     */
    private ProdutoResponseDTO mapAndEnrichProduto(Produto produto) {
        // O mapper já copia o nome do arquivo (ex: "foto123.jpg") para o DTO.
        // Não fazemos nada com a URL aqui. Deixamos o dado "cru".
        ProdutoResponseDTO dto = produtoMapper.toResponseDTO(produto);

        // 2. Calcula e define os dados de estoque.
        Integer estoqueFisicoTotal = movimentacaoEstoqueProdutoRepository.findSaldoByProduto(produto);
        List<Estoque> estoquesAtuais = estoqueRepository.findAllByProduto(produto);
        int estoqueDistribuidoTotal = estoquesAtuais.stream()
                .mapToInt(Estoque::getQuantidade)
                .sum();
        int estoqueDisponivelParaAlocar = estoqueFisicoTotal - estoqueDistribuidoTotal;

        dto.setEstoqueFisicoTotal(estoqueFisicoTotal);
        dto.setEstoqueDistribuidoTotal(estoqueDistribuidoTotal);
        dto.setEstoqueDisponivelParaAlocar(estoqueDisponivelParaAlocar);

        return dto;
    }

    /**
     * Busca uma entidade {@link Produto} pelo seu ID, lançando uma exceção se não for encontrada.
     *
     * @param id O ID do produto a ser buscado.
     * @return A entidade {@link Produto} encontrada.
     * @throws ProdutoNaoEncontradoException se o produto com o ID especificado não for encontrado.
     */
    private Produto findProdutoById(Long id) {
        return produtoRepository.findById(id)
                .orElseThrow(() -> new ProdutoNaoEncontradoException(id));
    }

    /**
     * Valida regras de negócio para a criação e atualização de produtos, como a unicidade de nome e SKU.
     *
     * @param requestDTO O DTO com os dados do produto.
     * @param produtoId O ID do produto que está sendo atualizado, ou {@code null} se for uma criação.
     * @throws ProdutoNomeDuplicadoException se o nome do produto já existir.
     * @throws ProdutoSkuDuplicadoException se o SKU do produto já existir.
     */
    private void validarRegrasDeNegocio(ProdutoRequestDTO requestDTO, Long produtoId) {
        if (produtoId == null) {
            if (produtoRepository.existsByNome(requestDTO.getNome())) {
                throw new ProdutoNomeDuplicadoException(requestDTO.getNome());
            }
            if (produtoRepository.existsBySku(requestDTO.getSku())) {
                throw new ProdutoSkuDuplicadoException(requestDTO.getSku());
            }
        } else {
            if (produtoRepository.existsByNomeAndIdNot(requestDTO.getNome(), produtoId)) {
                throw new ProdutoNomeDuplicadoException(requestDTO.getNome());
            }
            if (produtoRepository.existsBySkuAndIdNot(requestDTO.getSku(), produtoId)) {
                throw new ProdutoSkuDuplicadoException(requestDTO.getSku());
            }
        }
    }

    /**
     * Valida campos obrigatórios para a criação e atualização completa (PUT) de um produto.
     *
     * @param requestDTO O DTO contendo os dados do produto.
     * @return Um mapa de erros (campo -> mensagem). O mapa estará vazio se não houver erros.
     */
    private Map<String, String> validarCamposObrigatorios(ProdutoRequestDTO requestDTO) {
        Map<String, String> errors = new HashMap<>();
        if (requestDTO.getNome() == null || requestDTO.getNome().isBlank()) {
            errors.put("nome", "Nome do produto é obrigatório");
        }
        if (requestDTO.getSku() == null || requestDTO.getSku().isBlank()) {
            errors.put("sku", "SKU do produto é obrigatório");
        }
        if (requestDTO.getTipoMateriaPrimaId() == null) {
            errors.put("tipoMateriaPrimaId", "Tipo de matéria-prima é obrigatório");
        }
        // Validação para o objeto de dimensões e seus campos internos
        if (requestDTO.getDimensoes() == null) {
            errors.put("dimensoesUnitarias", "Dimensões são obrigatórias");
        } else {
            if (requestDTO.getDimensoes().getLarguraCm() == null) {
                errors.put("dimensoesUnitarias.larguraCm", "Largura é obrigatória");
            }
            if (requestDTO.getDimensoes().getComprimentoCm() == null) {
                errors.put("dimensoesUnitarias.comprimentoCm", "Comprimento é obrigatório");
            }
        }
        return errors;
    }
}
