package br.gov.ce.detran.treinamento.spring;

import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import static org.assertj.core.api.Assertions.assertThat;

class DashboardViewTest {
    @Test
    void renderizaDashboardCompletoComFiltroEAssets() throws Exception {
        try (var context = new SpringApplicationBuilder(Application.class)
                .profiles("test").properties("server.port=0").run()) {
            int port = ((WebServerApplicationContext) context).getWebServer().getPort();
            var client = HttpClient.newHttpClient();
            for (String tipo : new String[]{"", "PF", "PJ"}) {
                var response = client.send(HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port + "/?tipoPessoa=" + tipo)).build(),
                        HttpResponse.BodyHandlers.ofString());
                assertThat(response.statusCode()).isEqualTo(200);
                assertThat(response.body()).contains("Proprietários por dia", "Tipos de veículo", "Ver números por dia", "</html>")
                        .doesNotContain("th:", "<%", "Exception");
                assertThat(response.body().split("class=\"dashboard-chart-day\"", -1)).hasSize(15);
            }
            for (String path : new String[]{"/css/app.css", "/img/DETRAN.svg", "/proprietarios", "/proprietarios/novo", "/veiculos", "/veiculos/novo", "/veiculos/consultar-placa", "/historico", "/log-proprietarios"}) {
                var response = client.send(HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port + path)).build(), HttpResponse.BodyHandlers.ofString());
                assertThat(response.statusCode()).as(path).isEqualTo(200);
                assertThat(response.body()).doesNotContain("Exception");
                if (path.equals("/veiculos/novo")) {
                    assertThat(response.body()).contains("Cadastrar proprietário", "id=\"dados-veiculo\"", "disabled", "hidden");
                    assertThat(response.body().indexOf("id=\"idProprietario\""))
                            .isLessThan(response.body().indexOf("id=\"placa\""));
                }
            }
        }
    }
}
