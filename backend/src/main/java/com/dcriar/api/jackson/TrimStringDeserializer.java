package com.dcriar.api.jackson;

import com.dcriar.domain.common.util.TrimTextNormalizer;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

/**
 * Desserializador Jackson padrão para remover espaços nas extremidades de qualquer String de entrada.
 */
public class TrimStringDeserializer extends JsonDeserializer<String> {

    @Override
    public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        return TrimTextNormalizer.trimToNull(parser.getValueAsString());
    }
}
