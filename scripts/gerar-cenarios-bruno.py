#!/usr/bin/env python3
"""Gera cenários e requisições Bruno. Execute da raiz; sem dependências externas."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DEST = ROOT / 'bruno/06 Cenarios de erro'
DEST.mkdir(exist_ok=True)
# Remove somente arquivos produzidos por este gerador em execuções anteriores.
for generated in DEST.glob('[0-9][0-9][0-9] *.bru'):
    if '  name: NEG-' in generated.read_text():
        generated.unlink()
CASES = []


def add(name, method, route, status, body=None, *, field=None, pre='', post='', extra='', content_type='application/json', note=''):
    number = len(CASES) + 1
    code = f'NEG-{number:03}'
    filename = f'{number:03} {name}.bru'
    checks = [f'  test("{code}: HTTP {status}", function () {{\n    expect(res.getStatus()).to.equal({status});\n  }});']
    if status >= 400:
        checks.append('''  test("Contrato de erro e ausencia de detalhes internos", function () {
    const b = res.getBody();
    expect(b.status).to.equal(res.getStatus());
    expect(b.mensagem).to.be.a("string").and.not.empty;
    expect(b.excecao).to.be.a("string").and.not.empty;
    expect(b.dataHora).to.match(/^\\d{2}\\/\\d{2}\\/\\d{4} \\d{2}:\\d{2}:\\d{2}$/);
    expect(b.erros).to.be.an("array");
    for (const erro of b.erros) {
      expect(erro.campo).to.be.a("string").and.not.empty;
      expect(erro.mensagem).to.be.a("string").and.not.empty;
    }
    expect(b).not.to.have.property("stackTrace");
    expect(b).not.to.have.property("trace");
    expect(b.mensagem).not.to.match(/org\\.hibernate|org\\.postgresql|INSERT INTO|UPDATE treino/i);
  });''')
    if field:
        checks.append(f'''  test("Campo rejeitado: {field}", function () {{
    expect(res.getBody().erros.some(e => e.campo === "{field}")).to.equal(true);
  }});''')
    if extra:
        checks.append('  test("Efeito esperado", function () {\n' + extra + '\n  });')
    payload = json.dumps(body, ensure_ascii=False) if isinstance(body, (dict, list)) else body
    # IDs de runtime precisam ser números no JSON enviado.
    if payload:
        for key in ['erroProprietarioId', 'erroVeiculoId', 'erroVeiculo2Id']:
            payload = payload.replace('"{{' + key + '}}"', '{{' + key + '}}')
    mode = 'json' if payload is not None else 'none'
    text = f'''meta {{
  name: {code} {name}
  type: http
  seq: {number}
}}

{method.lower()} {{
  url: {{{{baseUrl}}}}{route}
  body: {mode}
  auth: none
}}
'''
    if payload is not None:
        text += f'\nheaders {{\n  Content-Type: {content_type}\n}}\n\nbody:json {{\n  {payload}\n}}\n'
    if pre:
        text += '\nscript:pre-request {\n' + pre + '\n}\n'
    if post:
        text += '\nscript:post-response {\n' + post + '\n}\n'
    text += '\ntests {\n' + '\n'.join(checks) + '\n}\n'
    (DEST / filename).write_text(text)
    CASES.append(dict(id=code, nome=name, metodo=method, rota=route, esperado=status,
                      corpo=payload, campo=field, arquivo=str((DEST / filename).relative_to(ROOT)), observacao=note))


P = '/api/proprietarios'
V = '/api/veiculos'
PI = P + '/{{erroProprietarioId}}'
VI = V + '/{{erroVeiculoId}}'
V2 = V + '/{{erroVeiculo2Id}}'
owner = {'cpfCnpj': '{{erroCpf}}', 'nome': 'Teste Cenarios Erro', 'cep': '60000000', 'estado': 'CE'}
vehicle = dict(tipo='CARRO', idProprietario='{{erroProprietarioId}}', anoFabricacao=2024,
               chassi='{{erroChassi}}', renavam='{{erroRenavam}}', placa='{{erroPlaca}}')

# Reutiliza o gerador de CPF da coleção, com variáveis próprias.
source = next((ROOT / 'bruno/01 Proprietarios').glob('01*.bru')).read_text()
setup = source.split('script:pre-request {\n', 1)[1].split('\n}\n', 1)[0]
for old, new in [('cpf', 'erroCpf'), ('chassi', 'erroChassi'), ('renavam', 'erroRenavam'), ('placa', 'erroPlaca'),
                 ('proprietarioId', 'erroProprietarioId'), ('veiculoId', 'erroVeiculoId'),
                 ('historicoId', 'erroHistoricoId'), ('logId', 'erroLogId')]:
    setup = setup.replace('"' + old + '"', '"' + new + '"')
setup += '\n  bru.setVar("erroChassi2", "SEG" + Date.now().toString());\n  bru.setVar("erroRenavam2", String((Number(bru.getVar("erroRenavam")) + 1) % 100000000000).padStart(11, "0"));\n  bru.setVar("erroPlaca2", "ZZZ" + String((Number(bru.getVar("erroPlaca").slice(-4)) + 1) % 10000).padStart(4, "0"));'
setup += '\n  bru.setVar("erroNovoChassi", "NOV" + Date.now().toString());\n  bru.setVar("erroNovoRenavam", String((Number(bru.getVar("erroRenavam")) + 2) % 100000000000).padStart(11, "0"));\n  bru.setVar("erroNovaPlaca", "YYY" + String((Number(bru.getVar("erroPlaca").slice(-4)) + 2) % 10000).padStart(4, "0"));'


def capture(key):
    return f'  if (res.getStatus() === 201) {{ bru.setVar("{key}", res.getBody().id); bru.setVar("{key}Original", res.getBody()); }}'


add('Preparar proprietario', 'POST', P, 201, owner, pre=setup, post=capture('erroProprietarioId'))
add('Preparar veiculo', 'POST', V, 201, vehicle, post=capture('erroVeiculoId'))
vehicle2 = {**vehicle, 'chassi': '{{erroChassi2}}', 'renavam': '{{erroRenavam2}}', 'placa': '{{erroPlaca2}}'}
add('Preparar segundo veiculo', 'POST', V, 201, vehicle2, post=capture('erroVeiculo2Id'))

# Todas as operações por ID: tipo, sinal, limites numéricos e inexistência.
for route, methods in [(P, ['GET', 'PATCH', 'DELETE']), (V, ['GET', 'PATCH', 'DELETE']),
                       ('/api/historico', ['GET']), ('/api/log-proprietarios', ['GET'])]:
    label = route.rsplit('/', 1)[1]
    for method in methods:
        for value, desc, expected in [('abc', 'texto', 400), ('0', 'zero', 400), ('-1', 'negativo', 400),
                                      ('1.5', 'decimal', 400), ('9223372036854775808', 'overflow', 400),
                                      ('2147483647', 'inexistente', 404)]:
            add(f'{label} {method} ID {desc}', method, route + '/' + value, expected,
                {} if method == 'PATCH' else None,
                note='ID positivo reservado para ausência; a execução pressupõe que 2147483647 não exista.' if expected == 404 else '')

# Obrigações e formatos, para POST e PATCH em ambos os recursos.
for field in ['cpfCnpj', 'nome']:
    for value, desc in [(None, 'nulo'), ('', 'vazio'), ('   ', 'branco')]:
        add(f'proprietario POST {field} {desc}', 'POST', P, 400, {**owner, field: value}, field=field)
    missing = {k: v for k, v in owner.items() if k != field}
    add(f'proprietario POST {field} ausente', 'POST', P, 400, missing, field=field)

for method, route in [('POST', P), ('PATCH', PI)]:
    base = owner if method == 'POST' else {}
    for field, value, desc in [('cpfCnpj', '11111111111', 'repetido'), ('cpfCnpj', '12345678900', 'digito invalido'),
                               ('cpfCnpj', '11222333000100', 'CNPJ invalido'), ('cep', '1234', 'curto'),
                               ('cep', 'ABCDEFGH', 'letras'), ('estado', 'CEA', 'longo'), ('estado', '1A', 'numero')]:
        add(f'proprietario {method} {field} {desc}', method, route, 400, {**base, field: value}, field=field)
    for field, size in [('nome', 150), ('rua', 150), ('numero', 20), ('cidade', 100), ('complemento', 255)]:
        add(f'proprietario {method} {field} excedido', method, route, 400, {**base, field: 'A' * (size + 1)}, field=field)

for field in ['nome', 'rua', 'numero', 'cep', 'cidade', 'estado', 'complemento', 'cpfCnpj']:
    add(f'proprietario PATCH {field} branco', 'PATCH', PI, 400, {field: '   '})
add('proprietario CPF duplicado', 'POST', P, 409, owner)
add('proprietario CPF imutavel', 'PATCH', PI, 400, {'cpfCnpj': '{{erroOutroCpf}}'},
    pre='  bru.setVar("erroOutroCpf", bru.getVar("erroCpf") === "52998224725" ? "11144477735" : "52998224725");')

for field in vehicle:
    for desc, value in [('nulo', None), ('ausente', None)]:
        body = {**vehicle, field: value}
        if desc == 'ausente':
            body.pop(field)
        add(f'veiculo POST {field} {desc}', 'POST', V, 400, body, field=field)
    if field not in ['idProprietario', 'anoFabricacao']:
        for value, desc in [('', 'vazio'), ('   ', 'branco')]:
            add(f'veiculo POST {field} {desc}', 'POST', V, 400, {**vehicle, field: value}, field=field)

for method, route in [('POST', V), ('PATCH', VI)]:
    base = vehicle if method == 'POST' else {}
    add(f'veiculo {method} tipo invalido', method, route, 400, {**base, 'tipo': 'CAMINHAO'}, field='tipo')
    for field in ['idProprietario', 'anoFabricacao']:
        for value, desc in [(0, 'zero'), (-1, 'negativo')]:
            add(f'veiculo {method} {field} {desc}', method, route, 400, {**base, field: value}, field=field)
        for value, desc in [('abc', 'texto'), (2147483648, 'overflow')]:
            add(f'veiculo {method} {field} {desc}', method, route, 400, {**base, field: value})
    for field, limit in [('chassi', 17), ('renavam', 11), ('placa', 7)]:
        add(f'veiculo {method} {field} excedido', method, route, 400, {**base, field: 'A' * (limit + 1)}, field=field)
    add(f'veiculo {method} dono inexistente', method, route, 404, {**base, 'idProprietario': 2147483647})

for field in ['tipo', 'chassi', 'renavam', 'placa']:
    add(f'veiculo PATCH {field} branco', 'PATCH', VI, 400, {field: '   '})
for field, var in [('chassi', 'erroChassi'), ('renavam', 'erroRenavam'), ('placa', 'erroPlaca')]:
    # Somente uma chave duplicada em cada POST, para isolar cada restrição.
    fresh = {**vehicle, 'chassi': '{{erroNovoChassi}}', 'renavam': '{{erroNovoRenavam}}', 'placa': '{{erroNovaPlaca}}'}
    add(f'veiculo POST {field} duplicado', 'POST', V, 409, {**fresh, field: '{{' + var + '}}'})
    add(f'veiculo PATCH {field} duplicado', 'PATCH', V2, 409, {field: '{{' + var + '}}'})

for method, route, label in [('POST', P, 'proprietario POST'), ('PATCH', PI, 'proprietario PATCH'),
                             ('POST', V, 'veiculo POST'), ('PATCH', VI, 'veiculo PATCH')]:
    for body, desc in [('{"campo":', 'JSON malformado'), ('null', 'corpo null'), ('[]', 'array'), (None, 'sem corpo')]:
        add(f'{label} {desc}', method, route, 400, body)
    add(f'{label} media type incorreto', method, route, 415, '{}', content_type='text/plain')

for route in [P, V, '/api/historico', '/api/log-proprietarios']:
    label = route.rsplit('/', 1)[1]
    add(f'{label} PUT nao permitido', 'PUT', route, 405, {}, extra='    expect(res.getHeader("allow")).to.be.a("string").and.not.empty;')
for route, idvar in [('/api/historico', 'historicoId'), ('/api/log-proprietarios', 'logId')]:
    # Não depende dos IDs da pasta 03: usar um número positivo basta para o roteamento 405.
    for method, suffix in [('POST', ''), ('PATCH', '/1'), ('DELETE', '/1')]:
        add(f'{route.rsplit("/", 1)[1]} {method} proibido', method, route + suffix, 405,
            {} if method != 'DELETE' else None)
add('Rota inexistente', 'GET', '/api/rota-que-nao-existe', 404)
add('Ordenacao por atributo inexistente', 'GET', V + '?sort=atributoInexistente,asc', 400,
    note='Expectativa de robustez: entrada de ordenação inválida deve gerar 400; 500 é falha da API.')
add('Paginacao com offset maior que limite JPA', 'GET', V + '?page=2147483647', 400,
    note='Expectativa de robustez: rejeitar página cujo offset não cabe no limite JPA; 500 é falha da API.')
for query, prop, expected in [('page=-1', 'number', 0), ('page=abc', 'number', 0),
                              ('size=0', 'size', 15), ('size=-1', 'size', 15),
                              ('size=abc', 'size', 15), ('size=100', 'size', 15)]:
    add('Paginacao normaliza ' + query.replace('=', ' '), 'GET', V + '?' + query, 200,
        extra=f'    expect(res.getBody().{prop}).to.equal({expected});\n    expect(res.getBody().content.length).to.be.at.most(15);',
        note='Comportamento atual do resolver Spring: valor inválido usa padrão e tamanho fica limitado a 15.')
add('Pagina fora dos resultados', 'GET', V + '?page=100000', 200,
    extra='    expect(res.getBody().content).to.deep.equal([]);')

# PATCH nulo/vazio é permitido pelo contrato e não deve apagar dados.
for route, label, key, fields in [(PI, 'proprietario', 'erroProprietarioId', ['nome', 'cpfCnpj']),
                                 (VI, 'veiculo', 'erroVeiculoId', ['tipo', 'placa', 'anoFabricacao'])]:
    for body, desc in [({}, 'vazio'), ({f: None for f in fields}, 'campos nulos')]:
        add(f'{label} PATCH {desc} preserva dados', 'PATCH', route, 200, body,
            extra=f'    expect(res.getBody()).to.deep.equal(bru.getVar("{key}Original"));')
    add(f'{label} falhas nao alteraram registro', 'GET', route, 200,
        extra=f'    expect(res.getBody()).to.deep.equal(bru.getVar("{key}Original"));')
add('segundo veiculo conflitos nao alteraram registro', 'GET', V2, 200,
    extra='    expect(res.getBody()).to.deep.equal(bru.getVar("erroVeiculo2IdOriginal"));')

add('Excluir veiculo de teste', 'DELETE', VI, 204)
for method in ['GET', 'PATCH', 'DELETE']:
    add(f'veiculo excluido {method}', method, VI, 404, {} if method == 'PATCH' else None)
add('Excluir proprietario e veiculo em cascata', 'DELETE', PI, 204)
for method in ['GET', 'PATCH', 'DELETE']:
    add(f'proprietario excluido {method}', method, PI, 404, {} if method == 'PATCH' else None)
for method in ['GET', 'PATCH', 'DELETE']:
    add(f'veiculo cascata excluido {method}', method, V2, 404, {} if method == 'PATCH' else None)
add('Cadastrar veiculo com dono inativo', 'POST', V, 404, vehicle)
add('CPF excluido continua reservado', 'POST', P, 409, owner)
add('Listagem oculta proprietario inativo', 'GET', P, 200,
    extra='    expect(res.getBody().some(p => p.id === bru.getVar("erroProprietarioId"))).to.equal(false);')
add('Listagem oculta veiculos inativos', 'GET', V, 200,
    extra='    expect(res.getBody().content.some(v => [bru.getVar("erroVeiculoId"), bru.getVar("erroVeiculo2Id")].includes(v.id))).to.equal(false);')
add('Historico preservado apos exclusao', 'GET', '/api/historico', 200,
    extra='    expect(res.getBody().some(h => h.entidade === "proprietario" && h.registroId === bru.getVar("erroProprietarioId") && h.operacao === "EXCLUSAO_LOGICA")).to.equal(true);')
add('Log preservado apos exclusao', 'GET', '/api/log-proprietarios', 200,
    extra='    expect(res.getBody().some(l => l.idProprietario === bru.getVar("erroProprietarioId"))).to.equal(true);')

(DEST / 'folder.bru').write_text('meta {\n  name: Cenarios de erro\n  seq: 6\n}\n')
(ROOT / 'docs/testes/cenarios.json').write_text(json.dumps(CASES, ensure_ascii=False, indent=2) + '\n')
print(f'{len(CASES)} cenários gerados em {DEST}')
