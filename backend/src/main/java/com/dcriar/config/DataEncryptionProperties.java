package com.dcriar.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "app.security.data-encryption")
public class DataEncryptionProperties {

    /**
     * Chave AES-256 em Base64 usada para cifrar dados sensíveis em repouso.
     */
    @NotBlank
    private String key;
}
