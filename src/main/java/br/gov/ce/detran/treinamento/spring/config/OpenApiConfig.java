package br.gov.ce.detran.treinamento.spring.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI openAPI(@Value("${spring.application.name}") String nome) {
        return new OpenAPI().info(new Info()
                .title(nome)
                .version("0.0.1-SNAPSHOT")
                .description("API REST de proprietários e veículos. Exclusão lógica; "
                        + "histórico e logs de proprietários disponíveis somente para consulta."));
    }
}
