package com.dcriar.config;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuração personalizada do Flyway para o ambiente de desenvolvimento.
 * <p>
 * Esta classe define um comportamento específico para o Flyway quando a aplicação
 * é executada com o perfil 'dev' ativo.
 */
@Configuration
public class FlywayConfig {

    /**
     * Define uma estratégia de migração para o Flyway que limpa (DROP) e recria (MIGRATE)
     * o banco de dados a cada inicialização da aplicação.
     * <p>
     * A anotação {@code @Profile("dev")} é crucial, pois garante que este comportamento
     * destrutivo NUNCA será executado em outros ambientes, como o de produção.
     * <p>
     * Este é o método ideal para automatizar o "reset" do banco de dados durante o
     * desenvolvimento, garantindo um estado limpo e previsível a cada execução.
     *
     * @return A estratégia de migração configurada.
     */
    @Bean
    @Profile("dev")
    public FlywayMigrationStrategy cleanMigrateStrategy() {
        return flyway -> {
            // 1. Limpa completamente o banco de dados (equivalente a um DROP em todas as tabelas, sequences, etc.)
            flyway.clean();
            // 2. Executa as migrações do zero (R__, V1__, etc.), recriando tudo.
            flyway.migrate();
        };
    }
}
