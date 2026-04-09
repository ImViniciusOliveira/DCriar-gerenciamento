package com.dcriar.domain.stock.service;

import com.dcriar.api.dto.request.stock.TipoEstruturalLoteFiltro;
import com.dcriar.api.dto.response.stock.AjusteLoteResumoDTO;
import com.dcriar.api.dto.request.stock.LoteMateriaPrimaRequestDTO;
import com.dcriar.api.dto.request.stock.MovimentacaoRequestDTO;
import com.dcriar.api.dto.response.stock.LoteMateriaPrimaResponseDTO;
import com.dcriar.api.dto.response.stock.MovimentacaoResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Interface que define o contrato para a lógica de negócio de Lotes de Matéria-Prima.
 */
public interface LoteMateriaPrimaService {

    /**
     * Cria um novo lote de matéria-prima no sistema.
     *
     * @param requestDTO O DTO com os dados para a criação do lote.
     * @return O {@link LoteMateriaPrimaResponseDTO} do lote recém-criado.
     */
    LoteMateriaPrimaResponseDTO create(LoteMateriaPrimaRequestDTO requestDTO);

    /**
     * Busca um lote de matéria-prima específico pelo seu ID.
     *
     * @param id O ID do lote a ser buscado.
     * @return O {@link LoteMateriaPrimaResponseDTO} do lote encontrado.
     */
    LoteMateriaPrimaResponseDTO findById(Long id);

    /**
     * Lista todos os lotes de matéria-prima, com a possibilidade de aplicar filtros e paginação.
     *
     * @param tipoMateriaPrimaId O ID do tipo de matéria-prima para filtrar (opcional).
     * @param apenasLotesPrincipais Se true, filtra apenas lotes que não são sobras (opcional).
     * @param pageable Objeto Pageable para informações de paginação e ordenação.
     * @return Uma {@link Page} de {@link LoteMateriaPrimaResponseDTO} com os lotes filtrados.
     */
    Page<LoteMateriaPrimaResponseDTO> findAll(Long tipoMateriaPrimaId, Boolean apenasLotesPrincipais, Pageable pageable);

    /**
     * Lista lotes prontos para a área de ajustes operacionais, com filtros de busca e tipo estrutural.
     */
    Page<AjusteLoteResumoDTO> findAllForAdjustments(String nomeMateriaPrima, TipoEstruturalLoteFiltro tipoEstrutural, Pageable pageable);

    /**
     * Registra uma nova movimentação de estoque para um lote de matéria-prima.
     *
     * @param loteId O ID do lote de matéria-prima.
     * @param requestDTO O DTO com os dados da movimentação.
     * @return O {@link MovimentacaoResponseDTO} da movimentação registrada.
     */
    MovimentacaoResponseDTO registrarMovimentacao(Long loteId, MovimentacaoRequestDTO requestDTO);

    /**
     * Lista todo o histórico de movimentações de um lote de matéria-prima específico.
     *
     * @param loteId O ID do lote cujo histórico será consultado.
     * @return Uma lista de {@link MovimentacaoResponseDTO} representando todas as movimentações do lote.
     */
    List<MovimentacaoResponseDTO> listarMovimentacoesPorLote(Long loteId);

    /**
     * Atualiza um lote de matéria-prima existente no sistema.
     * <p>
     * Utiliza o método {@code updateFrom} da entidade para centralizar regras de negócio de atualização.
     *
     * @param id O ID do lote a ser atualizado.
     * @param requestDTO O DTO com os dados para atualização do lote.
     * @return O {@link LoteMateriaPrimaResponseDTO} do lote atualizado.
     */
    LoteMateriaPrimaResponseDTO update(Long id, LoteMateriaPrimaRequestDTO requestDTO);

    /**
     * Exclui um lote de matéria-prima pelo seu ID.
     *
     * @param id O ID do lote a ser excluído.
     */
    void delete(Long id);
}
