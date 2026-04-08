package com.dcriar.api.mapper.sales;

import com.dcriar.domain.common.util.CountrySupport;
import com.dcriar.domain.common.util.BrazilStateSupport;
import com.dcriar.domain.common.util.HumanTextDisplayFormatter;
import com.dcriar.domain.sales.entity.Venda;
import com.dcriar.domain.sales.entity.enums.ModoLocalidadeVenda;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

/**
 * Formata campos de apresentação da venda sem concentrar regra de exibição no mapper.
 */
@Component
public class VendaDisplayFormatter {

    @Named("formatHumanText")
    public String formatHumanText(String value) {
        return HumanTextDisplayFormatter.format(value);
    }

    @Named("formatCountry")
    public String formatCountry(Venda venda) {
        return CountrySupport.toDisplayName(venda.getPais());
    }

    @Named("formatCity")
    public String formatCity(Venda venda) {
        return HumanTextDisplayFormatter.format(venda.getCidade());
    }

    @Named("formatState")
    public String formatState(Venda venda) {
        if (CountrySupport.isBrazil(venda.getPais())) {
            return BrazilStateSupport.toUf(venda.getEstado());
        }
        return HumanTextDisplayFormatter.format(venda.getEstado());
    }

    @Named("resolveLocationMode")
    public ModoLocalidadeVenda resolveLocationMode(Venda venda) {
        return CountrySupport.resolveLocationMode(venda.getPais());
    }
}
