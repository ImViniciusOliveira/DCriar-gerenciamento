package com.dcriar.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Mapeia as propriedades de configuração do MinIO a partir do arquivo application-prod.yml.
 * <p>
 * Esta classe utiliza {@link ConfigurationProperties} para agrupar de forma segura e
 * tipada todas as configurações relacionadas ao MinIO, como URL, credenciais e nome do bucket.
 * A anotação {@code @Configuration} a registra como um bean gerenciado pelo Spring.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {

    /**
     * A URL base do servidor MinIO. Ex: localhost:9000
     */
    private String url;

    /**
     * A chave de acesso (access key) para autenticação no MinIO.
     */
    private String accessKey;

    /**
     * A chave secreta (secret key) para autenticação no MinIO.
     */
    private String secretKey;

    /**
     * O nome do bucket onde os arquivos serão armazenados.
     */
    private String bucketName;
}
