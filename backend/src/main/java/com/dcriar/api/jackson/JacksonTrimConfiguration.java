package com.dcriar.api.jackson;

import com.fasterxml.jackson.core.JsonParser;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração global do Jackson para remover espaços nas extremidades de qualquer String recebida pela API.
 */
@Configuration
public class JacksonTrimConfiguration {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer trimStringCustomizer() {
        return builder -> builder
                .deserializerByType(String.class, new TrimStringDeserializer())
                .featuresToEnable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
    }
}
