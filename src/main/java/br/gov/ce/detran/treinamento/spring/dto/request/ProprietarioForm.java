package br.gov.ce.detran.treinamento.spring.dto.request;

import br.gov.ce.detran.treinamento.spring.dto.response.ProprietarioResponseDTO;
import br.gov.ce.detran.treinamento.spring.util.validation.CpfCnpj;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Modelo de formulário web; converte a entrada para os DTOs da camada de serviço. */
public class ProprietarioForm {
    @NotBlank(message = "Informe o CPF/CNPJ.") @CpfCnpj
    private String cpfCnpj;
    @NotBlank(message = "Informe o nome.") @Size(max = 150)
    private String nome;
    @Size(max = 150) private String rua;
    @Size(max = 20) private String numero;
    @Pattern(regexp = "^$|[0-9.\\-]{8,10}$", message = "CEP deve conter 8 dígitos.")
    private String cep;
    @Size(max = 100) private String cidade;
    @Pattern(regexp = "^$|[A-Za-z]{2}$", message = "Estado deve conter a UF com 2 letras.")
    private String estado;
    @Size(max = 255) private String complemento;

    public static ProprietarioForm from(ProprietarioResponseDTO dados) {
        var form = new ProprietarioForm();
        form.cpfCnpj = dados.cpfCnpj(); form.nome = dados.nome(); form.rua = dados.rua();
        form.numero = dados.numero(); form.cep = dados.cep(); form.cidade = dados.cidade();
        form.estado = dados.estado(); form.complemento = dados.complemento();
        return form;
    }

    public ProprietarioRequestDTO paraCadastro() {
        return new ProprietarioRequestDTO(cpfCnpj, nome, rua, numero, cep, cidade, estado, complemento);
    }

    public ProprietarioAtualizacaoRequestDTO paraAtualizacao() {
        return new ProprietarioAtualizacaoRequestDTO(cpfCnpj, nome, rua, numero, cep, cidade, estado, complemento);
    }

    public String getCpfCnpj() { return cpfCnpj; }
    public void setCpfCnpj(String cpfCnpj) { this.cpfCnpj = cpfCnpj; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getRua() { return rua; }
    public void setRua(String rua) { this.rua = rua; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public String getCep() { return cep; }
    public void setCep(String cep) { this.cep = cep; }
    public String getCidade() { return cidade; }
    public void setCidade(String cidade) { this.cidade = cidade; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getComplemento() { return complemento; }
    public void setComplemento(String complemento) { this.complemento = complemento; }
}
