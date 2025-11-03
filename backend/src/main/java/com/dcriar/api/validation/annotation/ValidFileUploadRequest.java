package com.dcriar.api.validation.annotation;

import com.dcriar.api.validation.validator.FileUploadRequestValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = FileUploadRequestValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidFileUploadRequest {
    String message() default "Requisição de upload de arquivo inválida.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
