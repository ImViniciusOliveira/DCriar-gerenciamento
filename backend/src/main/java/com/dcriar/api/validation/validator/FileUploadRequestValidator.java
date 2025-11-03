package com.dcriar.api.validation.validator;

import com.dcriar.api.dto.request.upload.FileUploadRequestDTO;
import com.dcriar.api.validation.annotation.ValidFileUploadRequest;

public class FileUploadRequestValidator extends BaseValidator<ValidFileUploadRequest, FileUploadRequestDTO> {

    @Override
    protected void validate(FileUploadRequestDTO dto) {
        addViolationIf(dto.getFile() == null || dto.getFile().isEmpty(), "O arquivo não pode estar vazio.", "file");
    }
}
