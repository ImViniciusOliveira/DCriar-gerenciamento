package com.dcriar.domain.sales.service.impl;

import com.dcriar.api.dto.response.sales.VendaAnaliseSerieTemporalResponseDTO;
import com.dcriar.domain.sales.repository.VendaRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VendaAnaliseServiceImplTest {

    @Test
    void consultarSerieTemporal_quandoPeriodoForUmDia_deveAgruparPorTurno() {
        LocalDate data = LocalDate.of(2026, 4, 16);
        VendaRepository vendaRepository = createRepositoryStub(List.of(
                new Object[]{LocalDateTime.of(2026, 4, 16, 9, 15), new BigDecimal("150.00")},
                new Object[]{LocalDateTime.of(2026, 4, 16, 14, 30), new BigDecimal("230.00")},
                new Object[]{LocalDateTime.of(2026, 4, 16, 20, 45), new BigDecimal("90.00")}
        ));

        VendaAnaliseServiceImpl service = new VendaAnaliseServiceImpl(vendaRepository);
        VendaAnaliseSerieTemporalResponseDTO response = service.consultarSerieTemporal(data, data);

        assertEquals("Receita por período do dia", response.getTrendLabel());
        assertEquals(3, response.getSerie().size());

        assertEquals("Manhã", response.getSerie().get(0).getLabel());
        assertEquals(new BigDecimal("150.00"), response.getSerie().get(0).getReceita());
        assertEquals(1L, response.getSerie().get(0).getTotalPedidos());

        assertEquals("Tarde", response.getSerie().get(1).getLabel());
        assertEquals(new BigDecimal("230.00"), response.getSerie().get(1).getReceita());
        assertEquals(1L, response.getSerie().get(1).getTotalPedidos());

        assertEquals("Noite", response.getSerie().get(2).getLabel());
        assertEquals(new BigDecimal("90.00"), response.getSerie().get(2).getReceita());
        assertEquals(1L, response.getSerie().get(2).getTotalPedidos());
    }

    @SuppressWarnings("unchecked")
    private VendaRepository createRepositoryStub(List<Object[]> serie) {
        return (VendaRepository) Proxy.newProxyInstance(
                VendaRepository.class.getClassLoader(),
                new Class[]{VendaRepository.class},
                (proxy, method, args) -> {
                    if ("findDataCriacaoEValorTotalPorPeriodo".equals(method.getName())) {
                        return serie;
                    }
                    if ("toString".equals(method.getName())) {
                        return "VendaRepositoryStub";
                    }
                    if ("hashCode".equals(method.getName())) {
                        return System.identityHashCode(proxy);
                    }
                    if ("equals".equals(method.getName())) {
                        return proxy == args[0];
                    }
                    throw new UnsupportedOperationException("Método não suportado no teste: " + method.getName());
                }
        );
    }
}
