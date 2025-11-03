package com.dcriar.api.controller.enums;

import com.dcriar.api.hateoas.enums.assembler.TipoMovimentacaoModelAssembler;
import com.dcriar.api.hateoas.enums.assembler.UnidadeDeMedidaModelAssembler;
import com.dcriar.api.hateoas.enums.model.TipoMovimentacaoModel;
import com.dcriar.api.hateoas.enums.model.UnidadeDeMedidaModel;
import com.dcriar.domain.stock.entity.enums.TipoMovimentacao;
import com.dcriar.domain.stock.entity.enums.UnidadeDeMedida;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

/**
 * Controlador para expor os enums relacionados ao domínio de Estoque.
 * <p>
 * A injeção de dependência é feita via campo (@Autowired) para quebrar uma dependência circular
 * de compilação com os assemblers, que precisam da referência da classe do controller.
 */
@RestController
@RequestMapping("/api/v1/enums/stock")
@Tag(name = "Enums - Estoque", description = "Endpoints para consulta de enums relacionados ao estoque")
public class StockEnumController {

    private final UnidadeDeMedidaModelAssembler unidadeDeMedidaModelAssembler;

    private final TipoMovimentacaoModelAssembler tipoMovimentacaoModelAssembler;

    public StockEnumController(UnidadeDeMedidaModelAssembler unidadeDeMedidaModelAssembler, TipoMovimentacaoModelAssembler tipoMovimentacaoModelAssembler) {
        this.unidadeDeMedidaModelAssembler = unidadeDeMedidaModelAssembler;
        this.tipoMovimentacaoModelAssembler = tipoMovimentacaoModelAssembler;
    }

    /**
     * Retorna uma coleção de todas as unidades de medida disponíveis no sistema, com links HATEOAS.
     *
     * @return Um {@link ResponseEntity} com um {@link CollectionModel} de {@link UnidadeDeMedidaModel}.
     */
    @GetMapping("/unidades-de-medida")
    @Operation(summary = "Listar todas as Unidades de Medida")
    @ApiResponse(responseCode = "200", description = "Lista de unidades de medida retornada com sucesso")
    public ResponseEntity<CollectionModel<UnidadeDeMedidaModel>> getUnidadesDeMedida() {
        return ResponseEntity.ok(unidadeDeMedidaModelAssembler.toCollectionModel(Arrays.asList(UnidadeDeMedida.values())));
    }

    /**
     * Retorna uma coleção de todos os tipos de movimentação de estoque disponíveis, com links HATEOAS.
     *
     * @return Um {@link ResponseEntity} com um {@link CollectionModel} de {@link TipoMovimentacaoModel}.
     */
    @GetMapping("/tipos-movimentacao")
    @Operation(summary = "Listar todos os Tipos de Movimentação de Estoque")
    @ApiResponse(responseCode = "200", description = "Lista de tipos de movimentação retornada com sucesso")
    public ResponseEntity<CollectionModel<TipoMovimentacaoModel>> getTiposMovimentacao() {
        return ResponseEntity.ok(tipoMovimentacaoModelAssembler.toCollectionModel(Arrays.asList(TipoMovimentacao.values())));
    }
}
