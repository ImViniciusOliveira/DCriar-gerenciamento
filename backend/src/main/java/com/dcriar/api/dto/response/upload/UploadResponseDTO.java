package com.dcriar.api.dto.response.upload;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO (Data Transfer Object) que representa a resposta de uma operação de upload de arquivo.
 * Contém a URL para download do arquivo que foi carregado.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadResponseDTO {

    /**
     * A URL completa a partir da qual o arquivo pode ser baixado.
     */
    @Schema(description = "A URL completa a partir da qual o arquivo pode ser baixado.", example = "http://localhost:8080/api/v1/uploads/download/my-image.jpg")
    private String fileDownloadUri;
}
