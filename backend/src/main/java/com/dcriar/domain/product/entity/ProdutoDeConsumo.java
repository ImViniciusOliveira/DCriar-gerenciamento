package com.dcriar.domain.product.entity;

import com.dcriar.domain.stock.entity.enums.UnidadeDeMedida;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Type;

import java.util.Map;

@Entity
@DiscriminatorValue("CONSUMO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
public class ProdutoDeConsumo extends Produto {

    @Column(name = "codigo_fabricante")
    private String codigoFabricante;

    @Enumerated(EnumType.STRING)
    @Column(name = "unidade_cadastro_consumo")
    private UnidadeDeMedida unidadeCadastroConsumo;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, String> especificacoes;

    @Override
    public String getTipoProduto() {
        return "CONSUMO";
    }
}
