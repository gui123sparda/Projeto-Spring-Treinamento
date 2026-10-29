const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');

const evento = {eventoId: 'evento-1', entidade: 'veiculo', operacao: 'CADASTRO', registroId: 1, descricao: 'ABC1D23'};

function elemento() {
    return {
        children: [], dataset: {}, listeners: {}, hidden: false, textContent: '',
        append(...filhos) { filhos.forEach(filho => { filho.parent = this; this.children.push(filho); }); },
        remove() { this.parent.children = this.parent.children.filter(filho => filho !== this); },
        get firstElementChild() { return this.children[0]; },
        setAttribute(nome, valor) { this[nome] = valor; },
        addEventListener(nome, funcao) { this.listeners[nome] = funcao; },
        click() { return this.listeners.click(); }
    };
}

async function carregar({permissao = 'granted', falhar = false, suporta = true, mensagem = ''} = {}) {
    const root = elemento(), botao = elemento();
    root.dataset = {url: '/notificacoes', mensagem};
    const nativas = [], timers = [], armazenamento = new Map();
    let pedidos = 0;
    class Notificacao {
        static permission = permissao;
        static async requestPermission() { pedidos++; this.permission = 'granted'; return 'granted'; }
        constructor(titulo, opcoes) {
            if (falhar) throw new TypeError('Indisponível');
            this.titulo = titulo; this.opcoes = opcoes; nativas.push(this);
        }
        close() {}
    }
    const contexto = {
        document: {
            getElementById: id => id === 'notificacoes' ? root : id === 'ativar-notificacoes' ? botao : null,
            createElement: elemento
        },
        sessionStorage: {getItem: chave => armazenamento.get(chave) ?? null, setItem: (chave, valor) => armazenamento.set(chave, valor)},
        location: {origin: 'http://localhost:8080'}, URL, AbortController,
        setTimeout: (fn, tempo) => { timers.push({fn, tempo}); return timers.length; }, clearTimeout() {},
        fetch: async () => ({ok: true, json: async () => ({cursor: 'evento-1', eventos: [evento, evento]})}),
        isSecureContext: true, focus() {}, addEventListener() {}
    };
    if (suporta) contexto.Notification = Notificacao;
    contexto.window = contexto;
    vm.runInNewContext(fs.readFileSync('src/main/resources/static/js/notificacoes.js', 'utf8'), contexto);
    await new Promise(resolve => setImmediate(resolve));
    return {root, botao, nativas, timers, pedidos: () => pedidos};
}

test('exibe aviso nativo com nome ou placa, sem campo na página e sem duplicar evento', async () => {
    const {root, nativas} = await carregar();
    assert.equal(nativas.length, 1);
    assert.match(nativas[0].opcoes.body, /ABC1D23 cadastrado/);
    assert.equal(nativas[0].opcoes.tag, 'evento-1');
    assert.equal(root.children.length, 0);
});

test('solicita permissão somente ao clicar no botão de ativação', async () => {
    const app = await carregar({permissao: 'default'});
    assert.equal(app.pedidos(), 0);
    assert.equal(app.botao.hidden, false);
    await app.botao.click();
    assert.equal(app.pedidos(), 1);
    assert.equal(app.botao.hidden, true);
});

test('usa aviso flutuante descartável se o navegador bloquear, não suportar ou falhar', async () => {
    for (const opcoes of [{permissao: 'denied'}, {suporta: false}, {falhar: true}]) {
        const {root, nativas, timers} = await carregar(opcoes);
        assert.equal(nativas.length, 0);
        assert.equal(root.children.length, 1);
        const aviso = root.children[0];
        assert.ok(aviso.children.some(filho => filho.textContent.includes('ABC1D23')));
        const fechar = aviso.children.find(filho => filho.type === 'button');
        assert.ok(fechar);
        assert.ok(timers.some(timer => timer.tempo === 10000));
        fechar.click();
        assert.equal(root.children.length, 0);
    }
});

test('mensagem da operação local também aparece como notificação', async () => {
    const {nativas} = await carregar({mensagem: 'Proprietário cadastrado com sucesso.'});
    assert.ok(nativas.some(aviso => aviso.opcoes.body === 'Proprietário cadastrado com sucesso.'));
});
