package com.dcriar.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configura a documentação da API gerada pelo Springdoc, baseada na especificação OpenAPI 3.
 * <p>
 * Esta classe é responsável por definir metadados essenciais da API, como título,
 * versão e informações de contato. Como a segurança será implementada posteriormente,
 * esta configuração inicial foca apenas nas informações públicas da API.
 */
@Configuration
public class OpenApiConfig {

    /**
     * Cria e personaliza o bean {@link OpenAPI} que define a estrutura da documentação.
     *
     * @return Um objeto {@link OpenAPI} configurado com as informações gerais da API.
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(createApiInfo());
    }

    /**
     * Cria o objeto de informações da API.
     *
     * @return Um objeto {@link Info} com os metadados da API.
     */
    private Info createApiInfo() {
        return new Info()
                .title("DCriar API - Controle de Estoque")
                .version("1.0.0")
                .description("API para gerenciamento de produtos, estoque e vendas da DCriar.")
                .contact(new Contact()
                        .name("Suporte DCriar")
                        .email("suporte@dcriar.com.br")
                        .url("https://www.dcriar.com.br/contato"))
                .license(new License()
                        .name("Licença Personalizada - Uso apenas para estudo, interno ou acadêmico. Proibida venda ou uso comercial sem autorização do autor.")
                        .url("https://www.dcriar.com.br/contato"));
    }
}