package br.gov.ce.detran.treinamento.spring.util;

import br.gov.ce.detran.treinamento.spring.dto.response.*;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.engine.util.JRLoader;
import java.io.*;
import java.util.*;

/** Adaptação dos campos e renderização dos modelos Jasper do projeto Servlet. */
public final class Relatorios {
    private Relatorios() {}
    public static String ocultarIds(String dados) {
        return dados.replaceAll("(?m)^\\h*\"(?:id|id_proprietario)\"\\h*:\\h*[^\\r\\n]*\\R?", "")
                .replaceAll(",(\\s*})", "$1");
    }

    public static Map<String, Object> linhaProprietario(ProprietarioResponseDTO p) {
        var linha = new HashMap<String, Object>();
        linha.put("id", p.id().longValue());
        linha.put("nome", texto(p.nome()));
        linha.put("cpfCnpj", documento(p.cpfCnpj()));
        linha.put("rua", texto(p.rua()));
        linha.put("numero", texto(p.numero()));
        linha.put("cep", cep(p.cep()));
        linha.put("cidade", texto(p.cidade()));
        linha.put("estado", texto(p.estado()));
        linha.put("complemento", texto(p.complemento()));
        return linha;
    }

    public static Map<String, Object> linhaVeiculo(VeiculoResponseDTO v) {
        var linha = new HashMap<String, Object>();
        linha.put("id", v.id().longValue());
        linha.put("placa", v.placa());
        linha.put("renavam", texto(v.renavam()));
        linha.put("chassi", texto(v.chassi()));
        linha.put("ano_fabricacao", v.anoFabricacao());
        linha.put("tipo", "MOTO".equals(v.tipo()) ? "Moto" : "Carro");
        linha.put("id_proprietario", v.idProprietario().longValue());
        return linha;
    }

    public static Map<String, Object> linhaAgrupada(ProprietarioResponseDTO p, VeiculoResponseDTO v) {
        var linha = new HashMap<String, Object>();
        linha.put("donoId", p.id().toString());
        linha.put("nome", p.nome());
        linha.put("documento", documento(p.cpfCnpj()));
        linha.put("endereco", texto(p.rua()) + ", " + texto(p.numero()) + " · " + texto(p.complemento())
                + " · " + texto(p.cidade()) + "/" + texto(p.estado()) + " · CEP " + cep(p.cep()));
        linha.put("dados", v == null ? "Sem veículos ativos vinculados." :
                ("MOTO".equals(v.tipo()) ? "Moto" : "Carro") + " • " + v.placa() + " • Ano " + v.anoFabricacao()
                        + "\nRENAVAM: " + v.renavam() + "\nCHASSI: " + v.chassi());
        linha.put("placaImagem", v == null ? null : PlacaImagem.renderizar(v.placa(), v.tipo(), p.estado(), p.cidade()));
        return linha;
    }

    private static String texto(String valor) { return valor == null || valor.isBlank() ? "—" : valor; }
    private static String cep(String valor) {
        return valor != null && valor.matches("[0-9]{8}") ? valor.substring(0, 5) + "-" + valor.substring(5) : texto(valor);
    }
    private static String documento(String valor) {
        if (valor.length() == 11) return valor.replaceFirst("(.{3})(.{3})(.{3})(.{2})", "$1.$2.$3-$4");
        if (valor.length() == 14) return valor.replaceFirst("(.{2})(.{3})(.{3})(.{4})(.{2})", "$1.$2.$3/$4-$5");
        return valor;
    }

    public static byte[] gerar(String modelo, String titulo, List<? extends Map<String, ?>> linhas, Map<String, Object> extras) {
        try (var arquivo = recurso("/jasper/" + modelo + ".jasper");
             var brasao = recurso("/static/img/BRASAO_DETRAN.png");
             var estado = recurso("/static/img/LOGO_ESTADO_CE.png");
             var rodape = recurso("/static/img/RODAPE.jpeg")) {
            var parametros = new HashMap<>(extras);
            parametros.put("TITULO", titulo);
            parametros.put("BRASAO_DETRAN", brasao);
            parametros.put("LOGO_ESTADO_CE", estado);
            parametros.put("RODAPE", rodape);
            var report = (JasperReport) JRLoader.loadObject(arquivo);
            return JasperExportManager.exportReportToPdf(JasperFillManager.fillReport(report, parametros,
                    new JRMapCollectionDataSource(new ArrayList<Map<String, ?>>(linhas))));
        } catch (IOException | JRException e) {
            throw new IllegalStateException("Não foi possível gerar o relatório " + modelo + ".", e);
        }
    }
    private static InputStream recurso(String caminho) {
        return Objects.requireNonNull(Relatorios.class.getResourceAsStream(caminho), "Recurso de relatório ausente: " + caminho);
    }
}
