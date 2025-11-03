package com.dcriar.api.controller.enums;

import com.dcriar.api.hateoas.enums.assembler.ModoCalculoModelAssembler;
import com.dcriar.api.hateoas.enums.model.ModoCalculoModel;
import com.dcriar.domain.production.enums.ModoCalculo;
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
 * Controlador para expor os enums relacionados ao domínio de Produção.
 * <p>
 * A injeção de dependência é feita via campo (@Autowired) para quebrar uma dependência circular
 * de compilação com os assemblers, que precisam da referência da classe do controller.
 */
@RestController
@RequestMapping("/api/v1/enums/production")
@Tag(name = "Enums - Produção", description = "Endpoints para consulta de enums relacionados à produção")
public class ProductionEnumController {

    private final ModoCalculoModelAssembler modoCalculoModelAssembler;

    public ProductionEnumController(ModoCalculoModelAssembler modoCalculoModelAssembler) {
        this.modoCalculoModelAssembler = modoCalculoModelAssembler;
    }

    /**
     * Retorna uma coleção de todos os modos de cálculo disponíveis, com links HATEOAS.
     *
     * @return Um {@link ResponseEntity} com um {@link CollectionModel} de {@link ModoCalculoModel}.
     */
    @GetMapping("/modos-calculo")
    @Operation(summary = "Listar todos os Modos de Cálculo")
    @ApiResponse(responseCode = "200", description = "Lista de modos de cálculo retornada com sucesso")
    public ResponseEntity<CollectionModel<ModoCalculoModel>> getModosCalculo() {
        return ResponseEntity.ok(modoCalculoModelAssembler.toCollectionModel(Arrays.asList(ModoCalculo.values())));
    }
}
