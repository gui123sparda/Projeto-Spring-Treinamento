(() => {
    const root = document.getElementById('notificacoes');
    if (!root) return;
    const chave = 'detran-notificacoes:' + root.dataset.url;
    const entidades = { proprietario: 'Proprietário', veiculo: 'Veículo' };
    const operacoes = { CADASTRO: 'cadastrado', ALTERACAO: 'editado', EXCLUSAO_LOGICA: 'excluído' };
    const estilos = { CADASTRO: 'cadastro', ALTERACAO: 'edicao', EXCLUSAO_LOGICA: 'exclusao' };
    let cursor = null, vistos = new Set();

    const ativar = document.getElementById('ativar-notificacoes');
    const suportaNotificacoes = () => 'Notification' in window && window.isSecureContext;
    function atualizarBotao() {
        if (!ativar) return;
        ativar.hidden = !suportaNotificacoes() || Notification.permission === 'granted';
        ativar.disabled = suportaNotificacoes() && Notification.permission === 'denied';
        ativar.textContent = ativar.disabled ? 'Notificações bloqueadas' : 'Ativar notificações';
        ativar.title = ativar.disabled ? 'Permita as notificações nas configurações deste site no navegador.' : '';
    }
    if (ativar) ativar.addEventListener('click', async () => {
        if (!suportaNotificacoes()) return;
        try { await Notification.requestPermission(); }
        catch (_) { /* O aviso flutuante continua disponível. */ }
        atualizarBotao();
    });
    window.addEventListener('focus', atualizarBotao);
    atualizarBotao();

    const valido = evento => evento && typeof evento.eventoId === 'string'
        && Object.hasOwn(entidades, evento.entidade) && Object.hasOwn(operacoes, evento.operacao)
        && Number.isInteger(evento.registroId);
    try {
        const salvo = JSON.parse(sessionStorage.getItem(chave));
        if (salvo) {
            cursor = typeof salvo.cursor === 'string' ? salvo.cursor : null;
            vistos = new Set(Array.isArray(salvo.vistos) ? salvo.vistos : []);
        }
    } catch (_) { /* O acompanhamento continua sem armazenamento local. */ }

    function flutuar(mensagem, estilo) {
        const aviso = document.createElement('div');
        aviso.className = `alert notificacao notificacao-${estilo}`;
        const texto = document.createElement('span');
        texto.textContent = mensagem;
        const fechar = document.createElement('button');
        fechar.type = 'button';
        fechar.className = 'notificacao-fechar';
        fechar.textContent = '×';
        fechar.setAttribute('aria-label', 'Fechar notificação');
        fechar.addEventListener('click', () => aviso.remove());
        aviso.append(texto, fechar);
        root.append(aviso);
        while (root.children.length > 3) root.firstElementChild.remove();
        setTimeout(() => aviso.remove(), 10000);
    }

    function exibir(mensagem, estilo, tag) {
        if (suportaNotificacoes() && Notification.permission === 'granted') {
            try {
                const aviso = new Notification('DETRAN · Treinamento CRUD', {body: mensagem, tag});
                aviso.onclick = () => { window.focus(); aviso.close(); };
                aviso.onerror = () => flutuar(mensagem, estilo);
                return;
            } catch (_) { /* Alguns navegadores não permitem notificações nesta plataforma. */ }
        }
        flutuar(mensagem, estilo);
    }

    function avisar(evento) {
        const descricao = typeof evento.descricao === 'string' ? evento.descricao.trim() : '';
        const identificacao = descricao ? ` ${descricao}` : '';
        exibir(`${entidades[evento.entidade]}${identificacao} ${operacoes[evento.operacao]} pelo aplicativo.`,
            estilos[evento.operacao], evento.eventoId);
    }

    if (root.dataset.mensagem) exibir(root.dataset.mensagem, 'cadastro');

    async function consultar() {
        const abortar = new AbortController();
        const timeout = setTimeout(() => abortar.abort(), 5000);
        try {
            const url = new URL(root.dataset.url, location.origin);
            if (cursor !== null) url.searchParams.set('apos', cursor);
            const resposta = await fetch(url, { cache: 'no-store', signal: abortar.signal, headers: { Accept: 'application/json' } });
            if (!resposta.ok) throw new Error('Consulta indisponível');
            const dados = await resposta.json();
            if (!Array.isArray(dados.eventos) || typeof dados.cursor !== 'string') throw new Error('Resposta inválida');
            for (const evento of dados.eventos) {
                if (!valido(evento) || vistos.has(evento.eventoId)) continue;
                vistos.add(evento.eventoId);
                avisar(evento);
            }
            vistos = new Set([...vistos].slice(-200));
            cursor = dados.cursor;
            try { sessionStorage.setItem(chave, JSON.stringify({ cursor, vistos: [...vistos] })); }
            catch (_) { /* O cursor em memória permanece disponível nesta página. */ }
        } catch (_) {
            // Mantém as mensagens existentes e tenta novamente na próxima consulta.
        } finally {
            clearTimeout(timeout);
            setTimeout(consultar, 2000);
        }
    }

    consultar();
})();
