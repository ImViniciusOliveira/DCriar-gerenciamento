package com.dcriar.api.controller.upload;

import com.dcriar.api.dto.response.upload.UploadResponseDTO;
import com.dcriar.domain.upload.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * Controller responsável por gerenciar o upload e download de arquivos de forma genérica.
 */
@RestController
@RequestMapping("/api/v1/uploads")
@RequiredArgsConstructor
@Tag(name = "Uploads", description = "Endpoints para upload e download de arquivos")
public class UploadController {

    private static final Logger log = LoggerFactory.getLogger(UploadController.class);

    private final FileStorageService fileStorageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Fazer upload de um arquivo")
    public ResponseEntity<UploadResponseDTO> uploadFile(
            @Parameter(description = "Arquivo enviado no campo multipart 'file'.")
            @RequestParam("file") MultipartFile file) {

        // Log minimal: sucesso ou falha
        try {
            String fileName = fileStorageService.storeFile(file);

            String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/v1/uploads/")
                    .pathSegment(fileName)
                    .toUriString();

            log.info("[UPLOAD] arquivo armazenado: {} (size={})", fileName, file.getSize());
            return ResponseEntity.ok(new UploadResponseDTO(fileDownloadUri));
        } catch (Exception e) {
            log.warn("[UPLOAD] falha ao armazenar arquivo: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{fileName:.+}")
    @Operation(summary = "Baixar um arquivo")
    public ResponseEntity<Resource> downloadFile(
            @Parameter(description = "Nome do arquivo a ser baixado.", example = "exemplo.png")
            @PathVariable String fileName, HttpServletRequest request) {

        String decodedName = URLDecoder.decode(fileName, StandardCharsets.UTF_8);
        Resource resource = fileStorageService.loadFileAsResource(decodedName);

        if (resource == null) {
            log.warn("[DOWNLOAD] arquivo não encontrado: {}", decodedName);
            return ResponseEntity.notFound().build();
        }

        String contentType = null;
        String filenameForHeader = resource.getFilename();
        if (filenameForHeader == null || filenameForHeader.isBlank()) {
            filenameForHeader = decodedName;
        }

        try {
            contentType = request.getServletContext().getMimeType(filenameForHeader);
        } catch (Exception ignored) {
        }

        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        String dispositionType = contentType.startsWith("image/") ? "inline" : "attachment";
        String encodedFilename = URLEncoder.encode(filenameForHeader, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        String contentDisposition = dispositionType + "; filename=\"" + filenameForHeader + "\"; filename*=UTF-8''" + encodedFilename;

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .body(resource);
    }

    // Endpoint debug para listar objetos no bucket (usar apenas em dev)
    @GetMapping("/debug/list")
    @Operation(summary = "DEBUG: lista objetos do bucket MinIO")
    public ResponseEntity<List<String>> debugListObjects() {
        try {
            String[] objects = fileStorageService.listAllObjects();
            log.info("[DEBUG] listAllObjects returned {} items", objects.length);
            return ResponseEntity.ok(Arrays.asList(objects));
        } catch (Exception e) {
            log.warn("[DEBUG] falha ao listar objetos: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}
