package com.dcriar.domain.common.persistence;

import com.dcriar.domain.common.security.SensitiveDataEncryptor;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Converter
public class EncryptedStringAttributeConverter implements AttributeConverter<String, String> {

    private static SensitiveDataEncryptor encryptor;

    @Autowired
    public void setEncryptor(SensitiveDataEncryptor encryptor) {
        EncryptedStringAttributeConverter.encryptor = encryptor;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return encryptor == null ? attribute : encryptor.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return encryptor == null ? dbData : encryptor.decrypt(dbData);
    }
}
