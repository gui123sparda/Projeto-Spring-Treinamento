package br.gov.ce.detran.treinamento.spring;

import br.gov.ce.detran.treinamento.spring.dto.request.*;
import br.gov.ce.detran.treinamento.spring.service.*;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RelatoriosWebTest {
    @Autowired WebApplicationContext context;
    @Autowired ProprietarioService proprietarios;
    @Autowired VeiculoService veiculos;
    @Autowired jakarta.persistence.EntityManager em;

    private MockMvc mvc() { return MockMvcBuilders.webAppContextSetup(context).build(); }
    private int dono() {
        return proprietarios.cadastrar(new ProprietarioRequestDTO("11144477735", "Relatório integração",
                "Rua teste", "10", "60000000", "Fortaleza", "CE", null)).id();
    }
    private int carro(int dono) {
        return veiculos.cadastrar(new VeiculoRequestDTO("CARRO", dono, 2024,
                "CHASSIRELATORIO", "98765432101", "RLT1A23")).id();
    }
    private String pdf(String url) throws Exception {
        var bytes = mvc().perform(get(java.net.URI.create(url))).andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment;")))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(bytes, 0, 5, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        try (var reader = new PdfReader(bytes)) {
            var extractor = new PdfTextExtractor(reader);
            var texto = new StringBuilder();
            for (int i = 1; i <= reader.getNumberOfPages(); i++) texto.append(extractor.getTextFromPage(i));
            return texto.toString().replaceAll("\\s+", " ");
        }
    }
    @Test void reutilizaModelosIndividuaisListasEAgrupadoComDadosReais() throws Exception {
        int id = dono(), veiculo = carro(id);
        em.flush();
        assertThat(pdf("/relatorio-proprietario?id=" + id)).contains("RELATÓRIO INTEGRAÇÃO", "111.444.777-35");
        assertThat(pdf("/relatorio-veiculo?id=" + veiculo)).contains("RLT1A23", "RELATÓRIO INTEGRAÇÃO", "CHASSIRELATORIO");
        assertThat(pdf("/relatorio-proprietarios?campo=nome&valor=RELATÓRIO%20INTEGRAÇÃO&tipoPessoa=PF"))
                .contains("RELATÓRIO INTEGRAÇÃO");
        assertThat(pdf("/relatorio-veiculos?campo=placa&valor=RLT1A23"))
                .contains("RLT1A23", "RELATÓRIO INTEGRAÇÃO");
        assertThat(pdf("/relatorio-agrupado?idProprietario=" + id)).contains("RELATÓRIO INTEGRAÇÃO", "RLT1A23");
        assertThat(pdf("/relatorio-proprietarios?campo=nome&valor=INEXISTENTE999"))
                .doesNotContain("RELATÓRIO INTEGRAÇÃO");
    }
    @Test void agrupadoSemVeiculosEHistoricosSeparadosComFiltros() throws Exception {
        int id = dono(); em.flush();
        assertThat(pdf("/relatorio-agrupado?idProprietario=" + id)).contains("Sem veículos ativos vinculados.");
        assertThat(pdf("/relatorio-historico?registroId=" + id)).contains("RELATÓRIO INTEGRAÇÃO");
        assertThat(pdf("/relatorio-historico-veiculos?registroId=2147483647"))
                .doesNotContain("RELATÓRIO INTEGRAÇÃO");
        mvc().perform(get("/historico-veiculos")).andExpect(status().isOk())
                .andExpect(model().attribute("historicoVeiculos", true));
        mvc().perform(get("/historico").param("registroId", "-1")).andExpect(status().isBadRequest());
        mvc().perform(get("/relatorio-historico").param("dataDe", "invalida")).andExpect(status().isBadRequest());
        mvc().perform(get("/relatorio-proprietario").param("id", "2147483647")).andExpect(status().isNotFound());
        mvc().perform(get("/relatorio-agrupado")).andExpect(status().isBadRequest());
    }
    @Test void exportaTodasAsPaginasEAplicaFiltrosDeVeiculos() throws Exception {
        int id = dono();
        for (int i = 0; i < 17; i++) veiculos.cadastrar(new VeiculoRequestDTO("CARRO", id, 2024,
                "RELATORIOCHASSI" + i, String.format("888888888%02d", i), String.format("PGN%04d", i)));
        em.flush();
        assertThat(pdf("/relatorio-veiculos?campo=placa&valor=PGN")).contains("PGN0000", "PGN0016", "Total de registros: 17");
        assertThat(pdf("/relatorio-veiculos?campo=placa&valor=PGN0016")).contains("PGN0016").doesNotContain("PGN0000");
        mvc().perform(get("/veiculos").param("campo", "placa").param("valor", "PGN0016"))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("PGN0016")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("PGN0000"))));
        mvc().perform(get("/veiculos").param("campo", "tipo").param("valor", "MOTO"))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("PGN0000"))));
    }
    @Test void atualizaProprietarioERetornaAListaDoTipoDePessoa() throws Exception {
        int id = dono();
        mvc().perform(post("/proprietarios/" + id).param("cpfCnpj", "11144477735")
                        .param("nome", "Nome atualizado"))
                .andExpect(redirectedUrl("/proprietarios?tipoPessoa=PF"));
    }

    @Test void detalhesDoProprietarioExibemSeusVeiculosEVoltamAListaFiltrada() throws Exception {
        int id = dono(); carro(id); em.flush();
        String html = mvc().perform(get("/proprietarios/" + id)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("RLT1A23", "/proprietarios?tipoPessoa=PF");
    }

    @Test void relatoriosSeparamPfPjEOmitemVeiculosExcluidos() throws Exception {
        int id = dono(), veiculo = carro(id);
        int empresa = proprietarios.cadastrar(new ProprietarioRequestDTO("12ABC34501DE35", "Empresa relatório",
                null, null, null, null, null, null)).id();
        em.flush();
        assertThat(pdf("/relatorio-proprietarios?tipoPessoa=PJ")).contains("EMPRESA RELATÓRIO").doesNotContain("RELATÓRIO INTEGRAÇÃO");
        assertThat(pdf("/relatorio-proprietarios?tipoPessoa=PF")).contains("RELATÓRIO INTEGRAÇÃO").doesNotContain("EMPRESA RELATÓRIO");
        assertThat(pdf("/relatorio-proprietario?id=" + empresa)).contains("12.ABC.345/01DE-35", "Relatório da empresa");
        veiculos.excluir(veiculo); em.flush(); em.clear();
        assertThat(pdf("/relatorio-agrupado?idProprietario=" + id)).contains("Sem veículos ativos vinculados.").doesNotContain("RLT1A23");
        assertThat(pdf("/relatorio-veiculos?campo=placa&valor=RLT1A23")).doesNotContain("RLT1A23");
        mvc().perform(get("/relatorio-veiculo").param("id", "" + veiculo)).andExpect(status().isNotFound());
    }
    @Test void historicoCompartilhaFiltrosDaTelaEPdfERespeitaLimitesDoDiaEmFortaleza() throws Exception {
        String[] horarios = {"2024-02-01T02:59:59Z", "2024-02-01T03:00:00Z", "2024-02-02T02:59:59Z", "2024-02-02T03:00:00Z"};
        String[] nomes = {"FORAANTES", "DENTROINICIO", "DENTROFIM", "FORADEPOIS"};
        for (int i = 0; i < horarios.length; i++) {
            var evento = new br.gov.ce.detran.treinamento.spring.model.Historico();
            evento.setEntidade("veiculo"); evento.setRegistroId(2147483646L); evento.setOperacao("CADASTRO");
            evento.setOcorridoEm(java.time.OffsetDateTime.parse(horarios[i])); evento.setUsuarioBanco("teste");
            evento.setDepois(java.util.Map.of("nome", nomes[i], "id", 123, "id_proprietario", 456));
            em.persist(evento);
        }
        em.flush();
        String filtros = "?registroId=2147483646&dataDe=2024-02-01&dataAte=2024-02-01&operacao=CADASTRO&busca=DENTRO";
        String html = mvc().perform(get(java.net.URI.create("/historico-veiculos" + filtros)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("DENTROINICIO", "DENTROFIM", "2 eventos").doesNotContain("FORAANTES", "FORADEPOIS");
        assertThat(pdf("/relatorio-historico-veiculos" + filtros)).contains("DENTROINICIO", "DENTROFIM")
                .doesNotContain("FORAANTES", "FORADEPOIS", "id_proprietario", "\"id\":");
        assertThat(pdf("/relatorio-historico" + filtros)).doesNotContain("DENTROINICIO", "DENTROFIM");
    }

    @Test void telasExibemAcoesDeRelatorioEExclusaoRetornaAosVeiculosDoDono() throws Exception {
        int id = dono(), veiculo = carro(id); em.flush();
        for (String url : new String[]{"/proprietarios", "/proprietarios/" + id,
                "/proprietarios/" + id + "/veiculos", "/veiculos", "/veiculos/consultar-placa?placa=RLT1A23", "/historico"}) {
            String html = mvc().perform(get(java.net.URI.create(url))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            assertThat(html).contains("/relatorio-").doesNotContain("th:");
        }
        mvc().perform(post("/veiculos/" + veiculo + "/excluir").param("idProprietarioRetorno", "" + id))
                .andExpect(redirectedUrl("/proprietarios/" + id + "/veiculos"));
    }
}
