#!/usr/bin/env python3
"""Documenta os cenários e resultados reais do Bruno e do Maven."""
import json
import xml.etree.ElementTree as ET
from datetime import datetime
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DOCS = ROOT / 'docs/testes'
cases = json.loads((DOCS / 'cenarios.json').read_text())
reports = json.loads((DOCS / 'resultado-bruno.json').read_text())
results = [r for iteration in reports for r in iteration['results']]


def checks(result):
    return [test for key in ['testResults', 'assertionResults', 'preRequestTestResults', 'postResponseTestResults']
            for test in result.get(key, [])]


def passed(result):
    return not result.get('error') and result.get('status') == 'pass' and all(t['status'] == 'pass' for t in checks(result))


by_file = {r['test']['filename']: r for r in results}
missing = [c['arquivo'] for c in cases if c['arquivo'].removeprefix('bruno/') not in by_file]
if missing:
    raise SystemExit('Cenários não executados: ' + ', '.join(missing))
count_ok = sum(passed(r) for r in results)
all_checks = [t for r in results for t in checks(r)]
bad = [r for r in results if not passed(r)]
java = []
for path in sorted((ROOT / 'target/surefire-reports').glob('TEST-*.xml')):
    suite = ET.parse(path).getroot()
    java.append((suite.attrib['name'], *(int(suite.attrib[k]) for k in ['tests', 'failures', 'errors', 'skipped'])))

lines = [
    '# Cenários e resultados dos testes da API', '',
    f'Relatório gerado em {datetime.now().astimezone().isoformat(timespec="seconds")}.', '',
    'Foram revisados todos os 14 endpoints públicos nos quatro controllers, os DTOs, serviços, '
    'restrições únicas e triggers de exclusão e auditoria do PostgreSQL. '
    'Os DTOs de criação de histórico e log não correspondem a endpoints públicos de escrita.', '',
    f'A execução HTTP final contém **{len(results)} requisições: {count_ok} aprovadas e {len(bad)} com falha**. '
    f'Das {len(all_checks)} asserções, {sum(t["status"] == "pass" for t in all_checks)} passaram. '
    f'A pasta nova tem **{len(cases)} cenários**, incluindo preparação, controles e limpeza. '
    'As 21 requisições anteriores também foram executadas.', '',
    f'A suíte Java executou **{sum(j[1] for j in java)} testes**, com '
    f'{sum(j[2] for j in java)} falhas, {sum(j[3] for j in java)} erros e {sum(j[4] for j in java)} ignorados.', '',
    '## Ambiente e execução', '',
    '- Java 25, Maven Wrapper e Bruno CLI 4.3.0.',
    '- Execução HTTP final: `http://localhost:8081`, PostgreSQL `dbtreino_test`, esquema `treino`.',
    '- Bruno: coleção existente `API Proprietários e Veículos`, pasta `06 Cenarios de erro` e ambiente `Testes`.',
    '- Cada erro testa HTTP, correspondência do status no corpo, mensagem, exceção, data/hora, array de erros e ausência de stack trace/SQL na mensagem.',
    '- Erros de validação de DTO também exigem o nome do campo rejeitado em `erros`. Isso é uma expectativa de qualidade explícita: hoje alguns PATCH não a atendem.',
    '- Cenários por ID usam `2147483647` como inexistente. Este ID estava ausente na execução; se existir em outro banco, escolha outro ID.',
    '- A pasta 06 prepara um proprietário e dois veículos próprios, preserva dados ao rejeitar alterações e faz exclusão lógica ao final. Não use execução paralela nem `--bail`, pois as últimas requisições fazem a limpeza.', '',
    'Na primeira execução exploratória, `spring-boot:run` com perfil `test` não incluiu '
    '`src/test/resources` e utilizou o banco local `dbtreino`. Os dados gerados nessa execução '
    'foram inativados pelo fluxo da própria coleção; histórico e logs permanecem por regra da API. '
    'A execução final usa URL explícita para `dbtreino_test`, conferida em `pg_stat_activity`.', '',
    '```sh',
    'export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64',
    'export PATH="$JAVA_HOME/bin:$PATH"',
    './mvnw test',
    './mvnw spring-boot:run \\',
    '  "-Dspring-boot.run.arguments=--server.port=8081 --spring.datasource.url=jdbc:postgresql://localhost:5432/dbtreino_test?currentSchema=treino --spring.jpa.show-sql=false"',
    '```', '',
    'Com a API iniciada, em outro terminal:', '', '```sh', 'cd bruno',
    'npx --yes @usebruno/cli@4.3.0 run --env Testes \\',
    '  --reporter-json ../docs/testes/resultado-bruno.json \\',
    '  --reporter-junit ../docs/testes/resultado-bruno.xml \\',
    '  --reporter-html ../docs/testes/resultado-bruno.html',
    'cd ..', 'python3 scripts/relatorio-cenarios.py', '```', '',
    'Para atualizar as requisições a partir da matriz: `python3 scripts/gerar-cenarios-bruno.py`. '
    'Esse comando substitui somente os arquivos gerados na pasta 06. '
    'No aplicativo Bruno, selecione `Testes` e execute a coleção inteira em ordem. '
    'A pasta 06 pode ser executada sozinha; cada requisição isolada que usa IDs exige a preparação 001–003 na mesma sessão.', '',
    '## Mapa de endpoints', '',
    '| Método | Rota | Cobertura |', '| --- | --- | --- |',
]
for route, methods in [('/api/proprietarios', ['GET', 'POST']), ('/api/proprietarios/{id}', ['GET', 'PATCH', 'DELETE']),
                       ('/api/veiculos', ['GET', 'POST']), ('/api/veiculos/{id}', ['GET', 'PATCH', 'DELETE']),
                       ('/api/historico', ['GET']), ('/api/historico/{id}', ['GET']),
                       ('/api/log-proprietarios', ['GET']), ('/api/log-proprietarios/{id}', ['GET'])]:
    for method in methods:
        lines.append(f'| {method} | `{route}` | Fluxo existente e cenários da pasta 06 |')

lines += ['', '## Falhas encontradas', '',
          'As expectativas abaixo foram mantidas para que as falhas continuem reproduzíveis. O código da API não foi alterado.', '',
          '1. **PATCH sem identificação dos campos inválidos (20 cenários):** '
          '`HandlerMethodValidationException` passa pelo tratamento genérico HTTP do `GlobalException`, '
          'que devolve `mensagem: "Validation failure"` e `erros: []`. O status 400 está correto, '
          'mas o consumidor não sabe qual campo corrigir. O POST fornece os campos; o PATCH deveria oferecer a mesma informação.',
          '2. **Ordenação inválida retorna 500:** `GET /api/veiculos?sort=atributoInexistente,asc` '
          'chega ao repositório sem validação de atributo e lança `PropertyReferenceException`. Esperado neste teste: 400.',
          '3. **Página extrema retorna 500:** `GET /api/veiculos?page=2147483647` ultrapassa o limite '
          'do offset JPA e lança `InvalidDataAccessApiUsageException`. Esperado neste teste: 400.', '',
          '| Cenário | HTTP real | Asserção que falhou |', '| --- | --- | --- |']
for r in bad:
    descriptions = '; '.join(t.get('description', t.get('name', '')) for t in checks(r) if t['status'] != 'pass')
    lines.append(f'| {r["name"]} | {r.get("response", {}).get("status", "sem resposta")} | {descriptions} |')

lines += ['', 'Parâmetros de paginação como `page=abc`, `page=-1`, `size=abc`, `size=0` e `size=-1` '
          'retornam 200 com os padrões do Spring; `size=100` é limitado a 15. Esses comportamentos '
          'foram caracterizados com testes de sucesso, pois a aplicação não define rejeição explícita desses valores.', '',
          'A requisição antiga `02 Veiculos/03 Buscar veiculo` usava o ID fixo `100000` embora esperasse 200. '
          'Foi corrigida para usar `{{veiculoId}}`, capturado no cadastro. IDs inexistentes estão cobertos por testes 404 próprios.', '',
          'Após a execução HTTP, o teste Java `exclusaoLogicaDoProprietarioInativaVeiculosPelaTrigger` '
          'falhou porque contava todas as exclusões do histórico, incluindo as deixadas pelo Bruno '
          '(esperava 2 e encontrou 7). A consulta da asserção foi limitada aos IDs e entidades '
          'criados pelo próprio teste, mantendo a exigência dos dois eventos. A suíte foi executada novamente após esse ajuste.', '',
          '## Resultado das 21 requisições anteriores', '',
          '| Requisição | HTTP | Resultado |', '| --- | --- | --- |']
for r in results:
    if not r['test']['filename'].startswith('06 '):
        lines.append(f'| {r["test"]["filename"]} | {r["response"]["status"]} | {"PASSOU" if passed(r) else "FALHOU"} |')

lines += ['', '## Matriz dos novos cenários', '',
          '| ID | Cenário | Método e rota | Esperado | Real | Resultado |', '| --- | --- | --- | --- | --- | --- |']
for c in cases:
    r = by_file[c['arquivo'].removeprefix('bruno/')]
    route = c['rota'].replace('|', '&#124;')
    lines.append(f'| [{c["id"]}](#{c["id"].lower()}) | {c["nome"]} | `{c["metodo"]} {route}` | {c["esperado"]} | {r["response"]["status"]} | {"PASSOU" if passed(r) else "FALHOU"} |')

lines += ['', '## Requisições e asserções de cada cenário', '',
          'Os valores `{{erro...}}` são gerados/capturados pela preparação. '
          'Os arquivos `.bru` abaixo contêm o teste executável completo, inclusive scripts e asserções.']
for c in cases:
    r = by_file[c['arquivo'].removeprefix('bruno/')]
    lines += ['', f'### {c["id"]}', '', f'**{c["nome"]}** — [Abrir teste](../../{c["arquivo"].replace(" ", "%20")}).', '',
              f'`{c["metodo"]} {{{{baseUrl}}}}{c["rota"]}` — esperado **{c["esperado"]}**, recebido **{r["response"]["status"]}**.', '']
    if c['corpo'] is None:
        lines.append('Corpo: ausente.')
    else:
        lines += ['Corpo enviado:', '', '```json', c['corpo'], '```']
    if c['observacao']:
        lines += ['', c['observacao']]
    lines += ['', 'Asserções executadas:', '']
    for t in checks(r):
        lines.append(f'- {"PASSOU" if t["status"] == "pass" else "FALHOU"}: {t.get("description", t.get("name", ""))}.')
    if not passed(r):
        lines += ['', 'Resposta recebida:', '', '```json', json.dumps(r['response'].get('data'), ensure_ascii=False, indent=2), '```']

lines += ['', '## Testes Java', '', '| Classe | Testes | Falhas | Erros | Ignorados |', '| --- | --- | --- | --- | --- |']
for name, tests, failures, errors, skipped in java:
    lines.append(f'| {name.rsplit(".", 1)[-1]} | {tests} | {failures} | {errors} | {skipped} |')
lines += ['', '## Evidências', '', '- [Relatório HTML](resultado-bruno.html).',
          '- [Respostas e asserções em JSON](resultado-bruno.json).', '- [JUnit XML](resultado-bruno.xml).',
          '- [Matriz estruturada dos cenários](cenarios.json).', '',
          'A suíte cobre validações e falhas funcionais previsíveis pelos endpoints atuais. '
          'Concorrência, carga, falhas de rede/banco e autenticação não foram simuladas. '
          'IDs de auditoria e dados fictícios constam nos relatórios. '
          'Exclusão é lógica: histórico e logs dos testes permanecem no banco.']
(DOCS / 'CENARIOS-E-RESULTADOS.md').write_text('\n'.join(lines) + '\n')
print(f'Relatório: {len(results)} requisições, {count_ok} aprovadas, {len(bad)} falhas; {len(cases)} cenários novos.')
