package com.dcriar.exception.custom;

/**
 * Exceção lançada quando há falha de configuração ou processamento da criptografia
 * dos dados sensíveis armazenados em repouso.
 */
public class DadosSensiveisCriptografiaException extends RuntimeException {

    private DadosSensiveisCriptografiaException(String message, Throwable cause) {
        super(message, cause);
    }

    private DadosSensiveisCriptografiaException(String message) {
        super(message);
    }

    public static DadosSensiveisCriptografiaException chaveInvalida() {
        return new DadosSensiveisCriptografiaException(
                "A chave de criptografia configurada para os dados sensíveis é inválida. Use uma chave AES-256 em Base64."
        );
    }

    public static DadosSensiveisCriptografiaException chaveNaoConfigurada() {
        return new DadosSensiveisCriptografiaException(
                "A chave de criptografia dos dados sensíveis não foi configurada. Defina DATA_ENCRYPTION_KEY no ambiente."
        );
    }

    public static DadosSensiveisCriptografiaException falhaAoCriptografar(Throwable cause) {
        return new DadosSensiveisCriptografiaException(
                "Falha ao criptografar um dado sensível da venda.",
                cause
        );
    }

    public static DadosSensiveisCriptografiaException falhaAoDescriptografar(Throwable cause) {
        return new DadosSensiveisCriptografiaException(
                "Falha ao descriptografar um dado sensível da venda.",
                cause
        );
    }
}
