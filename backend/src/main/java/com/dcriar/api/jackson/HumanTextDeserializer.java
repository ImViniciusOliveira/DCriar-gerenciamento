package com.dcriar.api.jackson;

import com.dcriar.domain.common.util.HumanTextNormalizer;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

/**
 * Desserializador Jackson para campos de texto humano.
 * <p>
 * Aplica a mesma normalização usada na camada de domínio sem afetar
 * campos técnicos da API inteira.
 */
public class HumanTextDeserializer extends JsonDeserializer<String> {

    @Override
    public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        return HumanTextNormalizer.normalize(parser.getValueAsString());
    }
}
