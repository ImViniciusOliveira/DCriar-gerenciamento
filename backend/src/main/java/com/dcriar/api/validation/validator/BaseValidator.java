package com.dcriar.api.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.lang.annotation.Annotation;

/**
 * Classe base abstrata para validadores customizados para simplificar a lógica de validação.
 * <p>
 * Esta classe gerencia o boilerplate de um {@link ConstraintValidator}, como a verificação
 * de nulidade do objeto, o controle do estado de validade e a construção de violações
 * de restrição.
 * <p>
 * As classes filhas devem implementar o método {@link #validate(Object)} para definir
 * a lógica de validação específica.
 *
 * @param <A> O tipo da anotação de restrição.
 * @param <T> O tipo do objeto a ser validado.
 */
public abstract class BaseValidator<A extends Annotation, T> implements ConstraintValidator<A, T> {

    private ConstraintValidatorContext context;
    private boolean isValid;

    @Override
    public final boolean isValid(T dto, ConstraintValidatorContext context) {
        if (dto == null) {
            return true;
        }

        this.context = context;
        this.isValid = true;
        this.context.disableDefaultConstraintViolation();

        validate(dto);

        return this.isValid;
    }

    /**
     * Método abstrato onde a lógica de validação específica deve ser implementada.
     *
     * @param dto O objeto a ser validado.
     */
    protected abstract void validate(T dto);

    /**
     * Adiciona uma violação de restrição se a condição fornecida for verdadeira.
     *
     * @param condition A condição a ser testada. Se for verdadeira, uma violação é adicionada.
     * @param message   A mensagem de erro para a violação.
     * @param fieldName O nome do campo que causou a violação.
     */
    protected void addViolationIf(boolean condition, String message, String fieldName) {
        if (condition) {
            context.buildConstraintViolationWithTemplate(message)
                    .addPropertyNode(fieldName)
                    .addConstraintViolation();
            isValid = false;
        }
    }
}
