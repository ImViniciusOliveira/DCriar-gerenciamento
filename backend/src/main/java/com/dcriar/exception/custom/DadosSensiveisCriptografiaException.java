package com.dcriar.exception.custom;

/**
 * Exceção lançada quando há falha de configuração ou processamento da criptografia
 * dos dados sensíveis armazenados em repouso.
 */
public class DadosSensiveisCriptografiaException extends RuntimeException {

    private final String codigo;

    private DadosSensiveisCriptografiaException(String message, Throwable cause) {
        super(message, cause);
        this.codigo = "CRIPTOGRAFIA_DADOS_SENSIVEIS";
    }

    private DadosSensiveisCriptografiaException(String message, String codigo) {
        super(message);
        this.codigo = codigo;
    }

    public static DadosSensiveisCriptografiaException chaveInvalida() {
        return new DadosSensiveisCriptografiaException(
                "A chave de criptografia configurada para os dados sensíveis é inválida. Use uma chave AES-256 em Base64.",
                "CHAVE_CRIPTOGRAFIA_INVALIDA"
        );
    }

    public static DadosSensiveisCriptografiaException chaveNaoConfigurada() {
        return new DadosSensiveisCriptografiaException(
                "A chave de criptografia dos dados sensíveis não foi configurada. Defina DATA_ENCRYPTION_KEY no ambiente.",
                "CHAVE_CRIPTOGRAFIA_NAO_CONFIGURADA"
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

    public String getCodigo() {
        return codigo;
    }
}
