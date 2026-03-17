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


import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {


    private final MinioProperties minioProperties;
    private MinioClient minioClient;


    @PostConstruct
    public void init() {
        this.minioClient = MinioClient.builder()
                .endpoint(minioProperties.getUrl())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .build();
        
        // Otimização: Verifica o bucket apenas na inicialização, não a cada upload.
        try {
            String bucket = minioProperties.getBucketName();
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("Bucket '{}' criado.", bucket);
            }
        } catch (Exception e) {
            log.warn("Aviso: Não foi possível verificar o bucket MinIO na inicialização: {}", e.getMessage());
        }
    }


    @Override
    public String storeFile(MultipartFile file) {
        String originalName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        
        // Higienização: Garante um nome de arquivo seguro para URLs (sem espaços ou caracteres estranhos)
        String extension = StringUtils.getFilenameExtension(originalName);
        String uniqueFileName = UUID.randomUUID() + (extension != null ? "." + extension : "");


        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(uniqueFileName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());


            log.info("[STORAGE] Arquivo salvo com sucesso: {}", uniqueFileName);
            return uniqueFileName;
        } catch (Exception e) {
            throw ArquivoStorageException.falhaAoArmazenar(originalName, e);
        }
    }


    @Override
    public Resource loadFileAsResource(String fileName) {
        try {
            // Busca Direta (Alta Performance): Sem loops, sem adivinhação. O nome deve ser exato.
            InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(fileName)
                            .build());
            return new InputStreamResource(stream);
        } catch (Exception e) {
            log.warn("[STORAGE] Arquivo não encontrado: {}", fileName);
            throw ArquivoNaoEncontradoException.noStorage(fileName);
        }
    }


    @Override
    public void deleteFile(String fileName) {
        if (!StringUtils.hasText(fileName)) return;
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(fileName)
                            .build());
        } catch (Exception e) {
            log.error("Erro não fatal ao excluir arquivo: {}", fileName, e);
        }
    }


    @Override
    public String extractFileName(String fileUrl) {
        if (!StringUtils.hasText(fileUrl)) return null;
        // Lógica simplificada assumindo que a URL gerada pelo sistema é padronizada
        int lastSlashIndex = fileUrl.lastIndexOf('/');
        return lastSlashIndex != -1 ? fileUrl.substring(lastSlashIndex + 1) : fileUrl;
    }


    @Override
    public String getFileUrl(String fileName) {
        return null; // Responsabilidade movida para o Assembler HATEOAS
    }


    @Override
    public String[] listAllObjects() {
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder().bucket(minioProperties.getBucketName()).build());
            List<String> names = new ArrayList<>();
            for (Result<Item> result : results) {
                names.add(result.get().objectName());
            }
            return names.toArray(new String[0]);
        } catch (Exception e) {
            return new String[0];
        }
    }
}
