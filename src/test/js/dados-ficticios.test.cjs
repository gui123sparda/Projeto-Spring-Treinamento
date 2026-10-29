const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const caminho = 'src/main/resources/static/js/dados-ficticios.js';

function carregar(document, math = Math) {
    const contexto = {module: {exports: {}}, Math: math, Event, document};
    vm.runInNewContext(fs.readFileSync(caminho, 'utf8'), contexto);
    return contexto.module.exports;
}

function validarDocumento(valor, empresa) {
    assert.match(valor, empresa ? /^\d{14}$/ : /^\d{11}$/);
    assert.ok(!/^(\d)\1+$/.test(valor));
    for (let tamanho = valor.length - 2; tamanho < valor.length; tamanho++) {
        let soma = 0;
        for (let i = 0; i < tamanho; i++) {
            const peso = empresa ? 2 + (tamanho - 1 - i) % 8 : tamanho + 1 - i;
            soma += Number(valor[i]) * peso;
        }
        const resultado = 11 - soma % 11;
        assert.equal(Number(valor[tamanho]), resultado >= 10 ? 0 : resultado);
    }
}

test('gera pessoas e empresas com documentos válidos e endereços completos', () => {
    const api = carregar();
    for (const empresa of [false, true]) {
        const documentos = new Set();
        for (let i = 0; i < 500; i++) {
            const dados = api.proprietario(empresa);
            validarDocumento(dados.cpfCnpj, empresa);
            documentos.add(dados.cpfCnpj);
            assert.ok(dados.nome.length > 0 && dados.nome.length <= 150);
            assert.match(dados.cep, /^\d{5}-?\d{3}$/);
            for (const nome of ['rua', 'numero', 'cidade', 'estado', 'complemento']) assert.ok(dados[nome]);
        }
        assert.ok(documentos.size > 450);
    }
});

test('gera CPF válido mesmo quando o sorteio produz somente zeros', () => {
    const api = carregar(undefined, {floor: Math.floor, random: () => 0});
    validarDocumento(api.documento(false), false);
});

test('gera veículos completos com placa, chassi e Renavam válidos', () => {
    const api = carregar();
    for (let i = 0; i < 500; i++) {
        const dados = api.veiculo();
        assert.match(dados.placa, /^[A-Z]{3}\d[A-Z0-9]\d{2}$/);
        assert.match(dados.chassi, /^[A-HJ-NPR-Z0-9]{17}$/);
        assert.match(dados.renavam, /^\d{11}$/);
        const pesos = [3, 2, 9, 8, 7, 6, 5, 4, 3, 2];
        const soma = pesos.reduce((total, peso, i) => total + peso * Number(dados.renavam[i]), 0);
        const resto = soma * 10 % 11;
        assert.equal(Number(dados.renavam[10]), resto === 10 ? 0 : resto);
        assert.ok(['CARRO', 'MOTO'].includes(dados.tipo));
        assert.ok(Number(dados.anoFabricacao) <= new Date().getFullYear());
        assert.ok(Number(dados.anoFabricacao) > 0);
    }
});

function formulario(tipo, empresa, edicao) {
    const nomes = tipo === 'veiculo'
        ? ['tipo', 'placa', 'renavam', 'chassi', 'anoFabricacao', 'idProprietario']
        : ['cpfCnpj', 'nome', 'rua', 'numero', 'cep', 'cidade', 'estado', 'complemento'];
    const campos = Object.fromEntries(nomes.map(nome => [nome, {
        value: nome === 'idProprietario' ? '42' : 'original',
        readOnly: nome === 'cpfCnpj' && edicao,
        dispatchEvent() {}
    }]));
    campos.tipoPessoa = {value: empresa ? 'PJ' : 'PF'};
    campos.namedItem = nome => campos[nome];
    const aviso = {textContent: ''};
    const radios = ['CARRO', 'MOTO'].map(value => ({value, type: 'radio', checked: false, dispatchEvent() {}}));
    if (tipo === 'veiculo') {
        campos.tipo = {
            get value() { return radios.find(radio => radio.checked)?.value ?? ''; },
            set value(valor) { radios.forEach(radio => { radio.checked = radio.value === valor; }); }
        };
    }
    const form = {elements: campos, querySelector: seletor => {
        if (seletor === '[data-aviso-ficticios]') return aviso;
        return radios.find(radio => seletor.includes(radio.value));
    }};
    let clicar;
    const botao = {
        dataset: {gerarFicticios: tipo},
        closest: () => form,
        addEventListener: (evento, handler) => { if (evento === 'click') clicar = handler; }
    };
    carregar({readyState: 'complete', querySelectorAll: () => [botao]});
    clicar();
    return {campos, aviso};
}

test('botão usa o tipo de pessoa e preserva documento somente leitura na edição', () => {
    for (const empresa of [false, true]) {
        const {campos, aviso} = formulario('proprietario', empresa, false);
        validarDocumento(campos.cpfCnpj.value, empresa);
        assert.notEqual(campos.nome.value, 'original');
        assert.ok(aviso.textContent.length > 0);
    }
    const {campos} = formulario('proprietario', false, true);
    assert.equal(campos.cpfCnpj.value, 'original');
    assert.notEqual(campos.nome.value, 'original');
});

test('botão de veículo preenche o tipo e mantém o proprietário selecionado', () => {
    const {campos} = formulario('veiculo', false, false);
    assert.equal(campos.idProprietario.value, '42');
    assert.ok(['CARRO', 'MOTO'].includes(campos.tipo.value));
    assert.match(campos.placa.value, /^[A-Z]{3}\d[A-Z0-9]\d{2}$/);
});
