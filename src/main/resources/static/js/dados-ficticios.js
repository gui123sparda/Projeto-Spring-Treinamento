(function (root) {
    'use strict';

    const inteiro = limite => Math.floor(Math.random() * limite);
    const escolher = valores => valores[inteiro(valores.length)];
    const digitos = quantidade => Array.from({length: quantidade}, () => inteiro(10)).join('');

    function documento(empresa) {
        let base = empresa ? digitos(8) + '0001' : digitos(9);
        // Evita CPFs com todos os dígitos iguais, inclusive nos extremos do sorteio.
        if (!empresa && /^(\d)\1+$/.test(base)) base = base.slice(0, 8) + (Number(base[8]) + 1) % 10;
        function verificador(valor) {
            const soma = [...valor].reduce((total, caractere, i) => {
                const peso = empresa ? 2 + (valor.length - 1 - i) % 8 : valor.length + 1 - i;
                return total + Number(caractere) * peso;
            }, 0);
            const resultado = 11 - soma % 11;
            return resultado >= 10 ? '0' : String(resultado);
        }
        const parcial = base + verificador(base);
        return parcial + verificador(parcial);
    }

    function proprietario(empresa) {
        return {
            cpfCnpj: documento(empresa),
            nome: empresa ? 'EMPRESA FICTÍCIA ' + digitos(6) + ' LTDA'
                : escolher(['ANA', 'JOÃO', 'MARIA', 'PAULO', 'LÚCIA']) + ' '
                    + escolher(['SILVA', 'COSTA', 'SANTOS', 'ALMEIDA']) + ' (FICTÍCIO)',
            rua: 'RUA DE DEMONSTRAÇÃO', numero: String(1 + inteiro(999)), cep: '60000-000',
            cidade: 'FORTALEZA', estado: 'CE', complemento: 'DADOS FICTÍCIOS PARA TESTE'
        };
    }

    function veiculo() {
        const letras = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';
        const caracteresChassi = 'ABCDEFGHJKLMNPRSTUVWXYZ0123456789';
        const baseRenavam = digitos(10);
        const soma = [...baseRenavam].reverse().reduce((total, caractere, i) =>
            total + Number(caractere) * (2 + i % 8), 0);
        const dv = 11 - soma % 11;
        return {
            tipo: escolher(['CARRO', 'MOTO']),
            placa: Array.from({length: 3}, () => escolher(letras)).join('') + inteiro(10)
                + (inteiro(2) ? escolher(letras) : inteiro(10)) + digitos(2),
            renavam: baseRenavam + (dv >= 10 ? 0 : dv),
            chassi: Array.from({length: 17}, () => escolher(caracteresChassi)).join(''),
            anoFabricacao: String(new Date().getFullYear() - inteiro(20))
        };
    }

    if (typeof module !== 'undefined') module.exports = {documento, proprietario, veiculo};
    if (!root.document) return;

    function iniciar() {
        root.document.querySelectorAll('[data-gerar-ficticios]').forEach(botao => {
            botao.addEventListener('click', () => {
                const form = botao.closest('form');
                const valores = botao.dataset.gerarFicticios === 'veiculo' ? veiculo()
                    : proprietario(form.elements.tipoPessoa.value === 'PJ');
                Object.entries(valores).forEach(([nome, valor]) => {
                    const campo = nome === 'tipo'
                        ? form.querySelector('input[name="tipo"][value="' + valor + '"]')
                        : form.elements.namedItem(nome);
                    if (campo && !campo.readOnly) {
                        if (campo.type === 'radio') campo.checked = true;
                        else campo.value = valor;
                        campo.dispatchEvent(new Event('input', {bubbles: true}));
                        campo.dispatchEvent(new Event('change', {bubbles: true}));
                    }
                });
                const aviso = form.querySelector('[data-aviso-ficticios]');
                if (aviso) aviso.textContent = 'Dados fictícios preenchidos. Revise antes de salvar. Não correspondem a uma identidade verificada.';
            });
        });
    }

    if (root.document.readyState === 'loading') root.document.addEventListener('DOMContentLoaded', iniciar);
    else iniciar();
}(typeof window !== 'undefined' ? window : globalThis));
