package br.gov.ce.detran.treinamento.spring.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Autoriza o cliente Flutter Web nas rotas REST da aplicação. */
@Configuration(proxyBeanMethods = false)
public class CorsConfig implements WebMvcConfigurer {
    private final String[] origensPermitidas;

    public CorsConfig(@Value("${app.cors.allowed-origin-patterns}") String[] origensPermitidas) {
        this.origensPermitidas = origensPermitidas;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(origensPermitidas)
                .allowedMethods("GET", "POST", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("Content-Type", "Accept")
                .maxAge(3600);
    }
}
