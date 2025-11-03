package com.dcriar.domain.upload.service.impl;

import com.dcriar.config.MinioProperties;
import com.dcriar.domain.upload.service.FileStorageService;
import com.dcriar.exception.custom.ArquivoNaoEncontradoException;
import com.dcriar.exception.custom.ArquivoStorageException;
import io.minio.*;
import io.minio.messages.Item;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.InputStream;
import java.util.Objects;
import java.util.UUID;

/**
 * Implementação principal do service de armazenamento de arquivos, utilizando MinIO.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    private final MinioProperties minioProperties;
    private MinioClient minioClient;

    /**
     * Inicializa o cliente MinIO e loga as configurações após a injeção de dependências.
     * Este método é executado automaticamente pelo Spring após a construção do serviço.
     */
    @PostConstruct
    public void init() {
        this.minioClient = MinioClient.builder()
                .endpoint(minioProperties.getUrl())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .build();

        // Garantir que o bucket exista; criar se não existir (robustez em dev e prod)
        try {
            String bucket = minioProperties.getBucketName();
            if (bucket != null && !bucket.isBlank()) {
                boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
                if (!exists) {
                    minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                    log.info("Bucket '{}' criado automaticamente no MinIO.", bucket);
                }
            } else {
                log.warn("Nome do bucket MinIO vazio; não será possível garantir existência do bucket.");
            }
        } catch (Exception e) {
            log.warn("Não foi possível garantir/criar o bucket no MinIO: {}", e.getMessage());
            // Não interrompe a inicialização; as operações de putObject tratarão erro caso ocorra
        }
    }

    @Override
    public String storeFile(MultipartFile file) {
        String originalFileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));

        if (originalFileName.contains("..")) {
            throw new ArquivoStorageException("Desculpe! O nome do arquivo contém uma sequência de caminho inválida: " + originalFileName);
        }

        try {
            // Keep the original filename (do not fully URL-encode it) to make testing easier.
            // We only replace leading/trailing spaces and normalize multiple spaces to single.
            String safeName = originalFileName.trim().replaceAll("\\s+", " ");
            String uniqueFileName = UUID.randomUUID() + "_" + safeName;

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(uniqueFileName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());

            log.info("[STORAGE] Arquivo salvo no MinIO: {} (size={}, contentType={})", uniqueFileName, file.getSize(), file.getContentType());
            return uniqueFileName;

        } catch (Exception e) {
            log.error("Falha ao tentar armazenar o arquivo {} no MinIO", originalFileName, e);
            throw new ArquivoStorageException("Não foi possível armazenar o arquivo " + originalFileName + ". Por favor, tente novamente!", e);
        }
    }

    @Override
    public Resource loadFileAsResource(String fileName) {
        // Vamos tentar várias variações para lidar com espaços, +, e codificação URL
        String[] candidates = buildCandidatesForFilename(fileName);
        // informational log to help debug only when level INFO is enabled
        log.info("[STORAGE] loadFileAsResource requested='{}' candidatesCount={}", fileName, candidates.length);
        for (String candidate : candidates) {
            try {
                log.info("[STORAGE] attempting to fetch object='{}' from bucket='{}'", candidate, minioProperties.getBucketName());
                InputStream stream = minioClient.getObject(
                        GetObjectArgs.builder()
                                .bucket(minioProperties.getBucketName())
                                .object(candidate)
                                .build());
                log.info("[STORAGE] object '{}' fetched successfully", candidate);
                return new InputStreamResource(stream);
            } catch (Exception e) {
                log.debug("[STORAGE] candidate '{}' failed: {}", candidate, e.getMessage());
                // tentar próximo candidato
            }
        }
        log.warn("[STORAGE] arquivo não encontrado após tentar variações: requested='{}' tried={}", fileName, candidates.length);
        throw new ArquivoNaoEncontradoException("Arquivo não encontrado: " + fileName);
    }

    /**
     * Gera uma lista de candidatos para tentar carregar do MinIO, a partir do nome solicitado.
     * Inclui o original, decodificado, variantes de espaços/+, e codificação URL.
     */
    private String[] buildCandidatesForFilename(String fileName) {
        java.util.Set<String> set = new java.util.LinkedHashSet<>();
        if (fileName != null) {
            set.add(fileName);
            try {
                String decoded = java.net.URLDecoder.decode(fileName, java.nio.charset.StandardCharsets.UTF_8);
                set.add(decoded);
            } catch (Exception ignored) {
            }
            // space <-> +
            set.add(fileName.replace("+", " "));
            set.add(fileName.replace(" ", "+"));
            // percent-encoding space
            set.add(fileName.replace(" ", "%20"));
            set.add(fileName.replace("%20", " "));
            // encoded full
            try {
                String encoded = java.net.URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8).replaceAll("\\+", "%20");
                set.add(encoded);
            } catch (Exception ignored) {
            }
        }
        return set.toArray(new String[0]);
    }

    @Override
    public void deleteFile(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return;
        }
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(fileName)
                            .build());
        } catch (Exception e) {
            log.error("Não foi possível excluir o arquivo do MinIO: {}", fileName, e);
            throw new ArquivoStorageException("Não foi possível excluir o arquivo " + fileName, e);
        }
    }

    @Override
    public String extractFileName(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return null;
        }
        try {
            java.net.URI uri = new java.net.URI(fileUrl);
            String path = uri.getPath();
            return path.substring(path.lastIndexOf('/') + 1);
        } catch (Exception e) {
            log.error("Erro ao extrair o nome do arquivo da URL: {}", fileUrl, e);
            return null;
        }
    }

    @Override
    public String getFileUrl(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }
        return UriComponentsBuilder.fromUriString(minioProperties.getUrl())
                .pathSegment(minioProperties.getBucketName(), fileName)
                .build()
                .toUriString();
    }

    @Override
    public String[] listAllObjects() {
        try {
            Iterable<Result<Item>> objects = minioClient.listObjects(
                    ListObjectsArgs.builder().bucket(minioProperties.getBucketName()).build());
            java.util.List<String> names = new java.util.ArrayList<>();
            for (Result<Item> result : objects) {
                try {
                    Item item = result.get();
                    names.add(item.objectName());
                } catch (Exception e) {
                    log.debug("[STORAGE] erro lendo item na listagem: {}", e.getMessage());
                }
            }
            return names.toArray(new String[0]);
        } catch (Exception e) {
            log.error("[STORAGE] falha ao listar objetos do bucket {}: {}", minioProperties.getBucketName(), e.getMessage());
            return new String[0];
        }
    }

}
