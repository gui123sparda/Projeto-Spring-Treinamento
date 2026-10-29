package br.gov.ce.detran.treinamento.spring.dto.request;
import br.gov.ce.detran.treinamento.spring.dto.response.VeiculoResponseDTO;
import jakarta.validation.constraints.*;
public class VeiculoForm {
    @NotBlank @Pattern(regexp = "CARRO|MOTO") private String tipo;
    @NotNull @Positive private Integer idProprietario;
    @NotNull @Positive private Integer anoFabricacao;
    @NotBlank @Size(max = 17) private String chassi;
    @NotBlank @Size(max = 11) private String renavam;
    @NotBlank @Size(max = 7) private String placa;
    public static VeiculoForm from(VeiculoResponseDTO dados) {
        var form = new VeiculoForm();
        form.tipo = dados.tipo();
        form.idProprietario = dados.idProprietario();
        form.anoFabricacao = dados.anoFabricacao();
        form.chassi = dados.chassi();
        form.renavam = dados.renavam();
        form.placa = dados.placa();
        return form;
    }
    public VeiculoRequestDTO paraCadastro() { return new VeiculoRequestDTO(tipo, idProprietario, anoFabricacao, chassi, renavam, placa); }
    public VeiculoAtualizacaoRequestDTO paraAtualizacao() { return new VeiculoAtualizacaoRequestDTO(tipo, idProprietario, anoFabricacao, chassi, renavam, placa); }
    public String getTipo() { return tipo; }
    public void setTipo(String valor) { this.tipo = valor; }
    public Integer getIdProprietario() { return idProprietario; }
    public void setIdProprietario(Integer valor) { this.idProprietario = valor; }
    public Integer getAnoFabricacao() { return anoFabricacao; }
    public void setAnoFabricacao(Integer valor) { this.anoFabricacao = valor; }
    public String getChassi() { return chassi; }
    public void setChassi(String valor) { this.chassi = valor; }
    public String getRenavam() { return renavam; }
    public void setRenavam(String valor) { this.renavam = valor; }
    public String getPlaca() { return placa; }
    public void setPlaca(String valor) { this.placa = valor; }
}
