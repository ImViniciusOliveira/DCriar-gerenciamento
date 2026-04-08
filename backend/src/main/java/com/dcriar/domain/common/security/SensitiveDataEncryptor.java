package com.dcriar.domain.common.security;

import com.dcriar.config.DataEncryptionProperties;
import com.dcriar.exception.custom.DadosSensiveisCriptografiaException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class SensitiveDataEncryptor {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String ALGORITHM = "AES";
    private static final String PREFIX = "ENCv1:";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final DataEncryptionProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    private SecretKeySpec secretKey;

    @PostConstruct
    void init() {
        try {
            byte[] decodedKey = Base64.getDecoder().decode(properties.getKey());
            if (decodedKey.length != 32) {
                throw DadosSensiveisCriptografiaException.chaveInvalida();
            }
            this.secretKey = new SecretKeySpec(decodedKey, ALGORITHM);
        } catch (IllegalArgumentException ex) {
            throw DadosSensiveisCriptografiaException.chaveInvalida();
        }
    }

    public String encrypt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));

            byte[] cipherText = cipher.doFinal(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(cipherText, 0, payload, iv.length, cipherText.length);

            return PREFIX + Base64.getEncoder().encodeToString(payload);
        } catch (Exception ex) {
            throw DadosSensiveisCriptografiaException.falhaAoCriptografar(ex);
        }
    }

    public String decrypt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        if (!value.startsWith(PREFIX)) {
            return value;
        }

        try {
            byte[] payload = Base64.getDecoder().decode(value.substring(PREFIX.length()));
            byte[] iv = Arrays.copyOfRange(payload, 0, IV_LENGTH);
            byte[] cipherText = Arrays.copyOfRange(payload, IV_LENGTH, payload.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));

            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw DadosSensiveisCriptografiaException.falhaAoDescriptografar(ex);
        }
    }
}
