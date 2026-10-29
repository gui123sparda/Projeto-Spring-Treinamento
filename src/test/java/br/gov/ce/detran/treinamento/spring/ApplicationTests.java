package br.gov.ce.detran.treinamento.spring;

import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import static org.assertj.core.api.Assertions.assertThat;

class ApplicationTests {
    @Test
    void contextLoads() {
        try (var context = new SpringApplicationBuilder(Application.class)
                .profiles("test").properties("server.port=0").run()) {
            assertThat(context.isActive()).isTrue();
        }
    }
}
