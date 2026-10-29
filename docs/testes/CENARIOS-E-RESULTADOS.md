# Cenários e resultados dos testes da API

Relatório gerado em 2026-10-23T11:34:20-03:00.

Foram revisados todos os 14 endpoints públicos nos quatro controllers, os DTOs, serviços, restrições únicas e triggers de exclusão e auditoria do PostgreSQL. Os DTOs de criação de histórico e log não correspondem a endpoints públicos de escrita.

A execução HTTP final contém **234 requisições: 212 aprovadas e 22 com falha**. Das 533 asserções, 511 passaram. A pasta nova tem **213 cenários**, incluindo preparação, controles e limpeza. As 21 requisições anteriores também foram executadas.

A suíte Java executou **78 testes**, com 0 falhas, 0 erros e 0 ignorados.

## Ambiente e execução

- Java 25, Maven Wrapper e Bruno CLI 4.3.0.
- Execução HTTP final: `http://localhost:8081`, PostgreSQL `dbtreino_test`, esquema `treino`.
- Bruno: coleção existente `API Proprietários e Veículos`, pasta `06 Cenarios de erro` e ambiente `Testes`.
- Cada erro testa HTTP, correspondência do status no corpo, mensagem, exceção, data/hora, array de erros e ausência de stack trace/SQL na mensagem.
- Erros de validação de DTO também exigem o nome do campo rejeitado em `erros`. Isso é uma expectativa de qualidade explícita: hoje alguns PATCH não a atendem.
- Cenários por ID usam `2147483647` como inexistente. Este ID estava ausente na execução; se existir em outro banco, escolha outro ID.
- A pasta 06 prepara um proprietário e dois veículos próprios, preserva dados ao rejeitar alterações e faz exclusão lógica ao final. Não use execução paralela nem `--bail`, pois as últimas requisições fazem a limpeza.

Na primeira execução exploratória, `spring-boot:run` com perfil `test` não incluiu `src/test/resources` e utilizou o banco local `dbtreino`. Os dados gerados nessa execução foram inativados pelo fluxo da própria coleção; histórico e logs permanecem por regra da API. A execução final usa URL explícita para `dbtreino_test`, conferida em `pg_stat_activity`.

```sh
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
./mvnw test
./mvnw spring-boot:run \
  "-Dspring-boot.run.arguments=--server.port=8081 --spring.datasource.url=jdbc:postgresql://localhost:5432/dbtreino_test?currentSchema=treino --spring.jpa.show-sql=false"
```

Com a API iniciada, em outro terminal:

```sh
cd bruno
npx --yes @usebruno/cli@4.3.0 run --env Testes \
  --reporter-json ../docs/testes/resultado-bruno.json \
  --reporter-junit ../docs/testes/resultado-bruno.xml \
  --reporter-html ../docs/testes/resultado-bruno.html
cd ..
python3 scripts/relatorio-cenarios.py
```

Para atualizar as requisições a partir da matriz: `python3 scripts/gerar-cenarios-bruno.py`. Esse comando substitui somente os arquivos gerados na pasta 06. No aplicativo Bruno, selecione `Testes` e execute a coleção inteira em ordem. A pasta 06 pode ser executada sozinha; cada requisição isolada que usa IDs exige a preparação 001–003 na mesma sessão.

## Mapa de endpoints

| Método | Rota | Cobertura |
| --- | --- | --- |
| GET | `/api/proprietarios` | Fluxo existente e cenários da pasta 06 |
| POST | `/api/proprietarios` | Fluxo existente e cenários da pasta 06 |
| GET | `/api/proprietarios/{id}` | Fluxo existente e cenários da pasta 06 |
| PATCH | `/api/proprietarios/{id}` | Fluxo existente e cenários da pasta 06 |
| DELETE | `/api/proprietarios/{id}` | Fluxo existente e cenários da pasta 06 |
| GET | `/api/veiculos` | Fluxo existente e cenários da pasta 06 |
| POST | `/api/veiculos` | Fluxo existente e cenários da pasta 06 |
| GET | `/api/veiculos/{id}` | Fluxo existente e cenários da pasta 06 |
| PATCH | `/api/veiculos/{id}` | Fluxo existente e cenários da pasta 06 |
| DELETE | `/api/veiculos/{id}` | Fluxo existente e cenários da pasta 06 |
| GET | `/api/historico` | Fluxo existente e cenários da pasta 06 |
| GET | `/api/historico/{id}` | Fluxo existente e cenários da pasta 06 |
| GET | `/api/log-proprietarios` | Fluxo existente e cenários da pasta 06 |
| GET | `/api/log-proprietarios/{id}` | Fluxo existente e cenários da pasta 06 |

## Falhas encontradas

As expectativas abaixo foram mantidas para que as falhas continuem reproduzíveis. O código da API não foi alterado.

1. **PATCH sem identificação dos campos inválidos (20 cenários):** `HandlerMethodValidationException` passa pelo tratamento genérico HTTP do `GlobalException`, que devolve `mensagem: "Validation failure"` e `erros: []`. O status 400 está correto, mas o consumidor não sabe qual campo corrigir. O POST fornece os campos; o PATCH deveria oferecer a mesma informação.
2. **Ordenação inválida retorna 500:** `GET /api/veiculos?sort=atributoInexistente,asc` chega ao repositório sem validação de atributo e lança `PropertyReferenceException`. Esperado neste teste: 400.
3. **Página extrema retorna 500:** `GET /api/veiculos?page=2147483647` ultrapassa o limite do offset JPA e lança `InvalidDataAccessApiUsageException`. Esperado neste teste: 400.

| Cenário | HTTP real | Asserção que falhou |
| --- | --- | --- |
| NEG-072 proprietario PATCH cpfCnpj repetido | 400 | Campo rejeitado: cpfCnpj |
| NEG-073 proprietario PATCH cpfCnpj digito invalido | 400 | Campo rejeitado: cpfCnpj |
| NEG-074 proprietario PATCH cpfCnpj CNPJ invalido | 400 | Campo rejeitado: cpfCnpj |
| NEG-075 proprietario PATCH cep curto | 400 | Campo rejeitado: cep |
| NEG-076 proprietario PATCH cep letras | 400 | Campo rejeitado: cep |
| NEG-077 proprietario PATCH estado longo | 400 | Campo rejeitado: estado |
| NEG-078 proprietario PATCH estado numero | 400 | Campo rejeitado: estado |
| NEG-079 proprietario PATCH nome excedido | 400 | Campo rejeitado: nome |
| NEG-080 proprietario PATCH rua excedido | 400 | Campo rejeitado: rua |
| NEG-081 proprietario PATCH numero excedido | 400 | Campo rejeitado: numero |
| NEG-082 proprietario PATCH cidade excedido | 400 | Campo rejeitado: cidade |
| NEG-083 proprietario PATCH complemento excedido | 400 | Campo rejeitado: complemento |
| NEG-127 veiculo PATCH tipo invalido | 400 | Campo rejeitado: tipo |
| NEG-128 veiculo PATCH idProprietario zero | 400 | Campo rejeitado: idProprietario |
| NEG-129 veiculo PATCH idProprietario negativo | 400 | Campo rejeitado: idProprietario |
| NEG-132 veiculo PATCH anoFabricacao zero | 400 | Campo rejeitado: anoFabricacao |
| NEG-133 veiculo PATCH anoFabricacao negativo | 400 | Campo rejeitado: anoFabricacao |
| NEG-136 veiculo PATCH chassi excedido | 400 | Campo rejeitado: chassi |
| NEG-137 veiculo PATCH renavam excedido | 400 | Campo rejeitado: renavam |
| NEG-138 veiculo PATCH placa excedido | 400 | Campo rejeitado: placa |
| NEG-181 Ordenacao por atributo inexistente | 500 | NEG-181: HTTP 400 |
| NEG-182 Paginacao com offset maior que limite JPA | 500 | NEG-182: HTTP 400 |

Parâmetros de paginação como `page=abc`, `page=-1`, `size=abc`, `size=0` e `size=-1` retornam 200 com os padrões do Spring; `size=100` é limitado a 15. Esses comportamentos foram caracterizados com testes de sucesso, pois a aplicação não define rejeição explícita desses valores.

A requisição antiga `02 Veiculos/03 Buscar veiculo` usava o ID fixo `100000` embora esperasse 200. Foi corrigida para usar `{{veiculoId}}`, capturado no cadastro. IDs inexistentes estão cobertos por testes 404 próprios.

Após a execução HTTP, o teste Java `exclusaoLogicaDoProprietarioInativaVeiculosPelaTrigger` falhou porque contava todas as exclusões do histórico, incluindo as deixadas pelo Bruno (esperava 2 e encontrou 7). A consulta da asserção foi limitada aos IDs e entidades criados pelo próprio teste, mantendo a exigência dos dois eventos. A suíte foi executada novamente após esse ajuste.

## Resultado das 21 requisições anteriores

| Requisição | HTTP | Resultado |
| --- | --- | --- |
| 01 Proprietarios/01 Cadastrar proprietario.bru | 201 | PASSOU |
| 01 Proprietarios/02 Listar proprietarios.bru | 200 | PASSOU |
| 01 Proprietarios/03 Buscar proprietario.bru | 200 | PASSOU |
| 01 Proprietarios/04 Atualizar proprietario.bru | 200 | PASSOU |
| 02 Veiculos/01 Cadastrar veiculo.bru | 201 | PASSOU |
| 02 Veiculos/02 Listar veiculos paginados.bru | 200 | PASSOU |
| 02 Veiculos/03 Buscar veiculo.bru | 200 | PASSOU |
| 02 Veiculos/04 Atualizar veiculo.bru | 200 | PASSOU |
| 03 Auditoria/01 Listar historico.bru | 200 | PASSOU |
| 03 Auditoria/02 Buscar historico.bru | 200 | PASSOU |
| 03 Auditoria/03 Listar logs.bru | 200 | PASSOU |
| 03 Auditoria/04 Buscar logs.bru | 200 | PASSOU |
| 04 Validacoes/01 Rejeitar CPF invalido.bru | 400 | PASSOU |
| 04 Validacoes/02 Rejeitar CPF duplicado.bru | 409 | PASSOU |
| 04 Validacoes/03 Rejeitar alteracao de CPF.bru | 400 | PASSOU |
| 04 Validacoes/04 Rejeitar tipo invalido.bru | 400 | PASSOU |
| 04 Validacoes/05 Rejeitar ID invalido.bru | 400 | PASSOU |
| 05 Exclusao/01 Excluir veiculo.bru | 204 | PASSOU |
| 05 Exclusao/02 Confirmar veiculo excluido.bru | 404 | PASSOU |
| 05 Exclusao/03 Excluir proprietario.bru | 204 | PASSOU |
| 05 Exclusao/04 Confirmar proprietario excluido.bru | 404 | PASSOU |

## Matriz dos novos cenários

| ID | Cenário | Método e rota | Esperado | Real | Resultado |
| --- | --- | --- | --- | --- | --- |
| [NEG-001](#neg-001) | Preparar proprietario | `POST /api/proprietarios` | 201 | 201 | PASSOU |
| [NEG-002](#neg-002) | Preparar veiculo | `POST /api/veiculos` | 201 | 201 | PASSOU |
| [NEG-003](#neg-003) | Preparar segundo veiculo | `POST /api/veiculos` | 201 | 201 | PASSOU |
| [NEG-004](#neg-004) | proprietarios GET ID texto | `GET /api/proprietarios/abc` | 400 | 400 | PASSOU |
| [NEG-005](#neg-005) | proprietarios GET ID zero | `GET /api/proprietarios/0` | 400 | 400 | PASSOU |
| [NEG-006](#neg-006) | proprietarios GET ID negativo | `GET /api/proprietarios/-1` | 400 | 400 | PASSOU |
| [NEG-007](#neg-007) | proprietarios GET ID decimal | `GET /api/proprietarios/1.5` | 400 | 400 | PASSOU |
| [NEG-008](#neg-008) | proprietarios GET ID overflow | `GET /api/proprietarios/9223372036854775808` | 400 | 400 | PASSOU |
| [NEG-009](#neg-009) | proprietarios GET ID inexistente | `GET /api/proprietarios/2147483647` | 404 | 404 | PASSOU |
| [NEG-010](#neg-010) | proprietarios PATCH ID texto | `PATCH /api/proprietarios/abc` | 400 | 400 | PASSOU |
| [NEG-011](#neg-011) | proprietarios PATCH ID zero | `PATCH /api/proprietarios/0` | 400 | 400 | PASSOU |
| [NEG-012](#neg-012) | proprietarios PATCH ID negativo | `PATCH /api/proprietarios/-1` | 400 | 400 | PASSOU |
| [NEG-013](#neg-013) | proprietarios PATCH ID decimal | `PATCH /api/proprietarios/1.5` | 400 | 400 | PASSOU |
| [NEG-014](#neg-014) | proprietarios PATCH ID overflow | `PATCH /api/proprietarios/9223372036854775808` | 400 | 400 | PASSOU |
| [NEG-015](#neg-015) | proprietarios PATCH ID inexistente | `PATCH /api/proprietarios/2147483647` | 404 | 404 | PASSOU |
| [NEG-016](#neg-016) | proprietarios DELETE ID texto | `DELETE /api/proprietarios/abc` | 400 | 400 | PASSOU |
| [NEG-017](#neg-017) | proprietarios DELETE ID zero | `DELETE /api/proprietarios/0` | 400 | 400 | PASSOU |
| [NEG-018](#neg-018) | proprietarios DELETE ID negativo | `DELETE /api/proprietarios/-1` | 400 | 400 | PASSOU |
| [NEG-019](#neg-019) | proprietarios DELETE ID decimal | `DELETE /api/proprietarios/1.5` | 400 | 400 | PASSOU |
| [NEG-020](#neg-020) | proprietarios DELETE ID overflow | `DELETE /api/proprietarios/9223372036854775808` | 400 | 400 | PASSOU |
| [NEG-021](#neg-021) | proprietarios DELETE ID inexistente | `DELETE /api/proprietarios/2147483647` | 404 | 404 | PASSOU |
| [NEG-022](#neg-022) | veiculos GET ID texto | `GET /api/veiculos/abc` | 400 | 400 | PASSOU |
| [NEG-023](#neg-023) | veiculos GET ID zero | `GET /api/veiculos/0` | 400 | 400 | PASSOU |
| [NEG-024](#neg-024) | veiculos GET ID negativo | `GET /api/veiculos/-1` | 400 | 400 | PASSOU |
| [NEG-025](#neg-025) | veiculos GET ID decimal | `GET /api/veiculos/1.5` | 400 | 400 | PASSOU |
| [NEG-026](#neg-026) | veiculos GET ID overflow | `GET /api/veiculos/9223372036854775808` | 400 | 400 | PASSOU |
| [NEG-027](#neg-027) | veiculos GET ID inexistente | `GET /api/veiculos/2147483647` | 404 | 404 | PASSOU |
| [NEG-028](#neg-028) | veiculos PATCH ID texto | `PATCH /api/veiculos/abc` | 400 | 400 | PASSOU |
| [NEG-029](#neg-029) | veiculos PATCH ID zero | `PATCH /api/veiculos/0` | 400 | 400 | PASSOU |
| [NEG-030](#neg-030) | veiculos PATCH ID negativo | `PATCH /api/veiculos/-1` | 400 | 400 | PASSOU |
| [NEG-031](#neg-031) | veiculos PATCH ID decimal | `PATCH /api/veiculos/1.5` | 400 | 400 | PASSOU |
| [NEG-032](#neg-032) | veiculos PATCH ID overflow | `PATCH /api/veiculos/9223372036854775808` | 400 | 400 | PASSOU |
| [NEG-033](#neg-033) | veiculos PATCH ID inexistente | `PATCH /api/veiculos/2147483647` | 404 | 404 | PASSOU |
| [NEG-034](#neg-034) | veiculos DELETE ID texto | `DELETE /api/veiculos/abc` | 400 | 400 | PASSOU |
| [NEG-035](#neg-035) | veiculos DELETE ID zero | `DELETE /api/veiculos/0` | 400 | 400 | PASSOU |
| [NEG-036](#neg-036) | veiculos DELETE ID negativo | `DELETE /api/veiculos/-1` | 400 | 400 | PASSOU |
| [NEG-037](#neg-037) | veiculos DELETE ID decimal | `DELETE /api/veiculos/1.5` | 400 | 400 | PASSOU |
| [NEG-038](#neg-038) | veiculos DELETE ID overflow | `DELETE /api/veiculos/9223372036854775808` | 400 | 400 | PASSOU |
| [NEG-039](#neg-039) | veiculos DELETE ID inexistente | `DELETE /api/veiculos/2147483647` | 404 | 404 | PASSOU |
| [NEG-040](#neg-040) | historico GET ID texto | `GET /api/historico/abc` | 400 | 400 | PASSOU |
| [NEG-041](#neg-041) | historico GET ID zero | `GET /api/historico/0` | 400 | 400 | PASSOU |
| [NEG-042](#neg-042) | historico GET ID negativo | `GET /api/historico/-1` | 400 | 400 | PASSOU |
| [NEG-043](#neg-043) | historico GET ID decimal | `GET /api/historico/1.5` | 400 | 400 | PASSOU |
| [NEG-044](#neg-044) | historico GET ID overflow | `GET /api/historico/9223372036854775808` | 400 | 400 | PASSOU |
| [NEG-045](#neg-045) | historico GET ID inexistente | `GET /api/historico/2147483647` | 404 | 404 | PASSOU |
| [NEG-046](#neg-046) | log-proprietarios GET ID texto | `GET /api/log-proprietarios/abc` | 400 | 400 | PASSOU |
| [NEG-047](#neg-047) | log-proprietarios GET ID zero | `GET /api/log-proprietarios/0` | 400 | 400 | PASSOU |
| [NEG-048](#neg-048) | log-proprietarios GET ID negativo | `GET /api/log-proprietarios/-1` | 400 | 400 | PASSOU |
| [NEG-049](#neg-049) | log-proprietarios GET ID decimal | `GET /api/log-proprietarios/1.5` | 400 | 400 | PASSOU |
| [NEG-050](#neg-050) | log-proprietarios GET ID overflow | `GET /api/log-proprietarios/9223372036854775808` | 400 | 400 | PASSOU |
| [NEG-051](#neg-051) | log-proprietarios GET ID inexistente | `GET /api/log-proprietarios/2147483647` | 404 | 404 | PASSOU |
| [NEG-052](#neg-052) | proprietario POST cpfCnpj nulo | `POST /api/proprietarios` | 400 | 400 | PASSOU |
| [NEG-053](#neg-053) | proprietario POST cpfCnpj vazio | `POST /api/proprietarios` | 400 | 400 | PASSOU |
| [NEG-054](#neg-054) | proprietario POST cpfCnpj branco | `POST /api/proprietarios` | 400 | 400 | PASSOU |
| [NEG-055](#neg-055) | proprietario POST cpfCnpj ausente | `POST /api/proprietarios` | 400 | 400 | PASSOU |
| [NEG-056](#neg-056) | proprietario POST nome nulo | `POST /api/proprietarios` | 400 | 400 | PASSOU |
| [NEG-057](#neg-057) | proprietario POST nome vazio | `POST /api/proprietarios` | 400 | 400 | PASSOU |
| [NEG-058](#neg-058) | proprietario POST nome branco | `POST /api/proprietarios` | 400 | 400 | PASSOU |
| [NEG-059](#neg-059) | proprietario POST nome ausente | `POST /api/proprietarios` | 400 | 400 | PASSOU |
| [NEG-060](#neg-060) | proprietario POST cpfCnpj repetido | `POST /api/proprietarios` | 400 | 400 | PASSOU |
| [NEG-061](#neg-061) | proprietario POST cpfCnpj digito invalido | `POST /api/proprietarios` | 400 | 400 | PASSOU |
| [NEG-062](#neg-062) | proprietario POST cpfCnpj CNPJ invalido | `POST /api/proprietarios` | 400 | 400 | PASSOU |
| [NEG-063](#neg-063) | proprietario POST cep curto | `POST /api/proprietarios` | 400 | 400 | PASSOU |
| [NEG-064](#neg-064) | proprietario POST cep letras | `POST /api/proprietarios` | 400 | 400 | PASSOU |
| [NEG-065](#neg-065) | proprietario POST estado longo | `POST /api/proprietarios` | 400 | 400 | PASSOU |
| [NEG-066](#neg-066) | proprietario POST estado numero | `POST /api/proprietarios` | 400 | 400 | PASSOU |
| [NEG-067](#neg-067) | proprietario POST nome excedido | `POST /api/proprietarios` | 400 | 400 | PASSOU |
| [NEG-068](#neg-068) | proprietario POST rua excedido | `POST /api/proprietarios` | 400 | 400 | PASSOU |
| [NEG-069](#neg-069) | proprietario POST numero excedido | `POST /api/proprietarios` | 400 | 400 | PASSOU |
| [NEG-070](#neg-070) | proprietario POST cidade excedido | `POST /api/proprietarios` | 400 | 400 | PASSOU |
| [NEG-071](#neg-071) | proprietario POST complemento excedido | `POST /api/proprietarios` | 400 | 400 | PASSOU |
| [NEG-072](#neg-072) | proprietario PATCH cpfCnpj repetido | `PATCH /api/proprietarios/{{erroProprietarioId}}` | 400 | 400 | FALHOU |
| [NEG-073](#neg-073) | proprietario PATCH cpfCnpj digito invalido | `PATCH /api/proprietarios/{{erroProprietarioId}}` | 400 | 400 | FALHOU |
| [NEG-074](#neg-074) | proprietario PATCH cpfCnpj CNPJ invalido | `PATCH /api/proprietarios/{{erroProprietarioId}}` | 400 | 400 | FALHOU |
| [NEG-075](#neg-075) | proprietario PATCH cep curto | `PATCH /api/proprietarios/{{erroProprietarioId}}` | 400 | 400 | FALHOU |
| [NEG-076](#neg-076) | proprietario PATCH cep letras | `PATCH /api/proprietarios/{{erroProprietarioId}}` | 400 | 400 | FALHOU |
| [NEG-077](#neg-077) | proprietario PATCH estado longo | `PATCH /api/proprietarios/{{erroProprietarioId}}` | 400 | 400 | FALHOU |
| [NEG-078](#neg-078) | proprietario PATCH estado numero | `PATCH /api/proprietarios/{{erroProprietarioId}}` | 400 | 400 | FALHOU |
| [NEG-079](#neg-079) | proprietario PATCH nome excedido | `PATCH /api/proprietarios/{{erroProprietarioId}}` | 400 | 400 | FALHOU |
| [NEG-080](#neg-080) | proprietario PATCH rua excedido | `PATCH /api/proprietarios/{{erroProprietarioId}}` | 400 | 400 | FALHOU |
| [NEG-081](#neg-081) | proprietario PATCH numero excedido | `PATCH /api/proprietarios/{{erroProprietarioId}}` | 400 | 400 | FALHOU |
| [NEG-082](#neg-082) | proprietario PATCH cidade excedido | `PATCH /api/proprietarios/{{erroProprietarioId}}` | 400 | 400 | FALHOU |
| [NEG-083](#neg-083) | proprietario PATCH complemento excedido | `PATCH /api/proprietarios/{{erroProprietarioId}}` | 400 | 400 | FALHOU |
| [NEG-084](#neg-084) | proprietario PATCH nome branco | `PATCH /api/proprietarios/{{erroProprietarioId}}` | 400 | 400 | PASSOU |
| [NEG-085](#neg-085) | proprietario PATCH rua branco | `PATCH /api/proprietarios/{{erroProprietarioId}}` | 400 | 400 | PASSOU |
| [NEG-086](#neg-086) | proprietario PATCH numero branco | `PATCH /api/proprietarios/{{erroProprietarioId}}` | 400 | 400 | PASSOU |
| [NEG-087](#neg-087) | proprietario PATCH cep branco | `PATCH /api/proprietarios/{{erroProprietarioId}}` | 400 | 400 | PASSOU |
| [NEG-088](#neg-088) | proprietario PATCH cidade branco | `PATCH /api/proprietarios/{{erroProprietarioId}}` | 400 | 400 | PASSOU |
| [NEG-089](#neg-089) | proprietario PATCH estado branco | `PATCH /api/proprietarios/{{erroProprietarioId}}` | 400 | 400 | PASSOU |
| [NEG-090](#neg-090) | proprietario PATCH complemento branco | `PATCH /api/proprietarios/{{erroProprietarioId}}` | 400 | 400 | PASSOU |
| [NEG-091](#neg-091) | proprietario PATCH cpfCnpj branco | `PATCH /api/proprietarios/{{erroProprietarioId}}` | 400 | 400 | PASSOU |
| [NEG-092](#neg-092) | proprietario CPF duplicado | `POST /api/proprietarios` | 409 | 409 | PASSOU |
| [NEG-093](#neg-093) | proprietario CPF imutavel | `PATCH /api/proprietarios/{{erroProprietarioId}}` | 400 | 400 | PASSOU |
| [NEG-094](#neg-094) | veiculo POST tipo nulo | `POST /api/veiculos` | 400 | 400 | PASSOU |
| [NEG-095](#neg-095) | veiculo POST tipo ausente | `POST /api/veiculos` | 400 | 400 | PASSOU |
| [NEG-096](#neg-096) | veiculo POST tipo vazio | `POST /api/veiculos` | 400 | 400 | PASSOU |
| [NEG-097](#neg-097) | veiculo POST tipo branco | `POST /api/veiculos` | 400 | 400 | PASSOU |
| [NEG-098](#neg-098) | veiculo POST idProprietario nulo | `POST /api/veiculos` | 400 | 400 | PASSOU |
| [NEG-099](#neg-099) | veiculo POST idProprietario ausente | `POST /api/veiculos` | 400 | 400 | PASSOU |
| [NEG-100](#neg-100) | veiculo POST anoFabricacao nulo | `POST /api/veiculos` | 400 | 400 | PASSOU |
| [NEG-101](#neg-101) | veiculo POST anoFabricacao ausente | `POST /api/veiculos` | 400 | 400 | PASSOU |
| [NEG-102](#neg-102) | veiculo POST chassi nulo | `POST /api/veiculos` | 400 | 400 | PASSOU |
| [NEG-103](#neg-103) | veiculo POST chassi ausente | `POST /api/veiculos` | 400 | 400 | PASSOU |
| [NEG-104](#neg-104) | veiculo POST chassi vazio | `POST /api/veiculos` | 400 | 400 | PASSOU |
| [NEG-105](#neg-105) | veiculo POST chassi branco | `POST /api/veiculos` | 400 | 400 | PASSOU |
| [NEG-106](#neg-106) | veiculo POST renavam nulo | `POST /api/veiculos` | 400 | 400 | PASSOU |
| [NEG-107](#neg-107) | veiculo POST renavam ausente | `POST /api/veiculos` | 400 | 400 | PASSOU |
| [NEG-108](#neg-108) | veiculo POST renavam vazio | `POST /api/veiculos` | 400 | 400 | PASSOU |
| [NEG-109](#neg-109) | veiculo POST renavam branco | `POST /api/veiculos` | 400 | 400 | PASSOU |
| [NEG-110](#neg-110) | veiculo POST placa nulo | `POST /api/veiculos` | 400 | 400 | PASSOU |
| [NEG-111](#neg-111) | veiculo POST placa ausente | `POST /api/veiculos` | 400 | 400 | PASSOU |
| [NEG-112](#neg-112) | veiculo POST placa vazio | `POST /api/veiculos` | 400 | 400 | PASSOU |
| [NEG-113](#neg-113) | veiculo POST placa branco | `POST /api/veiculos` | 400 | 400 | PASSOU |
| [NEG-114](#neg-114) | veiculo POST tipo invalido | `POST /api/veiculos` | 400 | 400 | PASSOU |
| [NEG-115](#neg-115) | veiculo POST idProprietario zero | `POST /api/veiculos` | 400 | 400 | PASSOU |
| [NEG-116](#neg-116) | veiculo POST idProprietario negativo | `POST /api/veiculos` | 400 | 400 | PASSOU |
| [NEG-117](#neg-117) | veiculo POST idProprietario texto | `POST /api/veiculos` | 400 | 400 | PASSOU |
| [NEG-118](#neg-118) | veiculo POST idProprietario overflow | `POST /api/veiculos` | 400 | 400 | PASSOU |
| [NEG-119](#neg-119) | veiculo POST anoFabricacao zero | `POST /api/veiculos` | 400 | 400 | PASSOU |
| [NEG-120](#neg-120) | veiculo POST anoFabricacao negativo | `POST /api/veiculos` | 400 | 400 | PASSOU |
| [NEG-121](#neg-121) | veiculo POST anoFabricacao texto | `POST /api/veiculos` | 400 | 400 | PASSOU |
| [NEG-122](#neg-122) | veiculo POST anoFabricacao overflow | `POST /api/veiculos` | 400 | 400 | PASSOU |
| [NEG-123](#neg-123) | veiculo POST chassi excedido | `POST /api/veiculos` | 400 | 400 | PASSOU |
| [NEG-124](#neg-124) | veiculo POST renavam excedido | `POST /api/veiculos` | 400 | 400 | PASSOU |
| [NEG-125](#neg-125) | veiculo POST placa excedido | `POST /api/veiculos` | 400 | 400 | PASSOU |
| [NEG-126](#neg-126) | veiculo POST dono inexistente | `POST /api/veiculos` | 404 | 404 | PASSOU |
| [NEG-127](#neg-127) | veiculo PATCH tipo invalido | `PATCH /api/veiculos/{{erroVeiculoId}}` | 400 | 400 | FALHOU |
| [NEG-128](#neg-128) | veiculo PATCH idProprietario zero | `PATCH /api/veiculos/{{erroVeiculoId}}` | 400 | 400 | FALHOU |
| [NEG-129](#neg-129) | veiculo PATCH idProprietario negativo | `PATCH /api/veiculos/{{erroVeiculoId}}` | 400 | 400 | FALHOU |
| [NEG-130](#neg-130) | veiculo PATCH idProprietario texto | `PATCH /api/veiculos/{{erroVeiculoId}}` | 400 | 400 | PASSOU |
| [NEG-131](#neg-131) | veiculo PATCH idProprietario overflow | `PATCH /api/veiculos/{{erroVeiculoId}}` | 400 | 400 | PASSOU |
| [NEG-132](#neg-132) | veiculo PATCH anoFabricacao zero | `PATCH /api/veiculos/{{erroVeiculoId}}` | 400 | 400 | FALHOU |
| [NEG-133](#neg-133) | veiculo PATCH anoFabricacao negativo | `PATCH /api/veiculos/{{erroVeiculoId}}` | 400 | 400 | FALHOU |
| [NEG-134](#neg-134) | veiculo PATCH anoFabricacao texto | `PATCH /api/veiculos/{{erroVeiculoId}}` | 400 | 400 | PASSOU |
| [NEG-135](#neg-135) | veiculo PATCH anoFabricacao overflow | `PATCH /api/veiculos/{{erroVeiculoId}}` | 400 | 400 | PASSOU |
| [NEG-136](#neg-136) | veiculo PATCH chassi excedido | `PATCH /api/veiculos/{{erroVeiculoId}}` | 400 | 400 | FALHOU |
| [NEG-137](#neg-137) | veiculo PATCH renavam excedido | `PATCH /api/veiculos/{{erroVeiculoId}}` | 400 | 400 | FALHOU |
| [NEG-138](#neg-138) | veiculo PATCH placa excedido | `PATCH /api/veiculos/{{erroVeiculoId}}` | 400 | 400 | FALHOU |
| [NEG-139](#neg-139) | veiculo PATCH dono inexistente | `PATCH /api/veiculos/{{erroVeiculoId}}` | 404 | 404 | PASSOU |
| [NEG-140](#neg-140) | veiculo PATCH tipo branco | `PATCH /api/veiculos/{{erroVeiculoId}}` | 400 | 400 | PASSOU |
| [NEG-141](#neg-141) | veiculo PATCH chassi branco | `PATCH /api/veiculos/{{erroVeiculoId}}` | 400 | 400 | PASSOU |
| [NEG-142](#neg-142) | veiculo PATCH renavam branco | `PATCH /api/veiculos/{{erroVeiculoId}}` | 400 | 400 | PASSOU |
| [NEG-143](#neg-143) | veiculo PATCH placa branco | `PATCH /api/veiculos/{{erroVeiculoId}}` | 400 | 400 | PASSOU |
| [NEG-144](#neg-144) | veiculo POST chassi duplicado | `POST /api/veiculos` | 409 | 409 | PASSOU |
| [NEG-145](#neg-145) | veiculo PATCH chassi duplicado | `PATCH /api/veiculos/{{erroVeiculo2Id}}` | 409 | 409 | PASSOU |
| [NEG-146](#neg-146) | veiculo POST renavam duplicado | `POST /api/veiculos` | 409 | 409 | PASSOU |
| [NEG-147](#neg-147) | veiculo PATCH renavam duplicado | `PATCH /api/veiculos/{{erroVeiculo2Id}}` | 409 | 409 | PASSOU |
| [NEG-148](#neg-148) | veiculo POST placa duplicado | `POST /api/veiculos` | 409 | 409 | PASSOU |
| [NEG-149](#neg-149) | veiculo PATCH placa duplicado | `PATCH /api/veiculos/{{erroVeiculo2Id}}` | 409 | 409 | PASSOU |
| [NEG-150](#neg-150) | proprietario POST JSON malformado | `POST /api/proprietarios` | 400 | 400 | PASSOU |
| [NEG-151](#neg-151) | proprietario POST corpo null | `POST /api/proprietarios` | 400 | 400 | PASSOU |
| [NEG-152](#neg-152) | proprietario POST array | `POST /api/proprietarios` | 400 | 400 | PASSOU |
| [NEG-153](#neg-153) | proprietario POST sem corpo | `POST /api/proprietarios` | 400 | 400 | PASSOU |
| [NEG-154](#neg-154) | proprietario POST media type incorreto | `POST /api/proprietarios` | 415 | 415 | PASSOU |
| [NEG-155](#neg-155) | proprietario PATCH JSON malformado | `PATCH /api/proprietarios/{{erroProprietarioId}}` | 400 | 400 | PASSOU |
| [NEG-156](#neg-156) | proprietario PATCH corpo null | `PATCH /api/proprietarios/{{erroProprietarioId}}` | 400 | 400 | PASSOU |
| [NEG-157](#neg-157) | proprietario PATCH array | `PATCH /api/proprietarios/{{erroProprietarioId}}` | 400 | 400 | PASSOU |
| [NEG-158](#neg-158) | proprietario PATCH sem corpo | `PATCH /api/proprietarios/{{erroProprietarioId}}` | 400 | 400 | PASSOU |
| [NEG-159](#neg-159) | proprietario PATCH media type incorreto | `PATCH /api/proprietarios/{{erroProprietarioId}}` | 415 | 415 | PASSOU |
| [NEG-160](#neg-160) | veiculo POST JSON malformado | `POST /api/veiculos` | 400 | 400 | PASSOU |
| [NEG-161](#neg-161) | veiculo POST corpo null | `POST /api/veiculos` | 400 | 400 | PASSOU |
| [NEG-162](#neg-162) | veiculo POST array | `POST /api/veiculos` | 400 | 400 | PASSOU |
| [NEG-163](#neg-163) | veiculo POST sem corpo | `POST /api/veiculos` | 400 | 400 | PASSOU |
| [NEG-164](#neg-164) | veiculo POST media type incorreto | `POST /api/veiculos` | 415 | 415 | PASSOU |
| [NEG-165](#neg-165) | veiculo PATCH JSON malformado | `PATCH /api/veiculos/{{erroVeiculoId}}` | 400 | 400 | PASSOU |
| [NEG-166](#neg-166) | veiculo PATCH corpo null | `PATCH /api/veiculos/{{erroVeiculoId}}` | 400 | 400 | PASSOU |
| [NEG-167](#neg-167) | veiculo PATCH array | `PATCH /api/veiculos/{{erroVeiculoId}}` | 400 | 400 | PASSOU |
| [NEG-168](#neg-168) | veiculo PATCH sem corpo | `PATCH /api/veiculos/{{erroVeiculoId}}` | 400 | 400 | PASSOU |
| [NEG-169](#neg-169) | veiculo PATCH media type incorreto | `PATCH /api/veiculos/{{erroVeiculoId}}` | 415 | 415 | PASSOU |
| [NEG-170](#neg-170) | proprietarios PUT nao permitido | `PUT /api/proprietarios` | 405 | 405 | PASSOU |
| [NEG-171](#neg-171) | veiculos PUT nao permitido | `PUT /api/veiculos` | 405 | 405 | PASSOU |
| [NEG-172](#neg-172) | historico PUT nao permitido | `PUT /api/historico` | 405 | 405 | PASSOU |
| [NEG-173](#neg-173) | log-proprietarios PUT nao permitido | `PUT /api/log-proprietarios` | 405 | 405 | PASSOU |
| [NEG-174](#neg-174) | historico POST proibido | `POST /api/historico` | 405 | 405 | PASSOU |
| [NEG-175](#neg-175) | historico PATCH proibido | `PATCH /api/historico/1` | 405 | 405 | PASSOU |
| [NEG-176](#neg-176) | historico DELETE proibido | `DELETE /api/historico/1` | 405 | 405 | PASSOU |
| [NEG-177](#neg-177) | log-proprietarios POST proibido | `POST /api/log-proprietarios` | 405 | 405 | PASSOU |
| [NEG-178](#neg-178) | log-proprietarios PATCH proibido | `PATCH /api/log-proprietarios/1` | 405 | 405 | PASSOU |
| [NEG-179](#neg-179) | log-proprietarios DELETE proibido | `DELETE /api/log-proprietarios/1` | 405 | 405 | PASSOU |
| [NEG-180](#neg-180) | Rota inexistente | `GET /api/rota-que-nao-existe` | 404 | 404 | PASSOU |
| [NEG-181](#neg-181) | Ordenacao por atributo inexistente | `GET /api/veiculos?sort=atributoInexistente,asc` | 400 | 500 | FALHOU |
| [NEG-182](#neg-182) | Paginacao com offset maior que limite JPA | `GET /api/veiculos?page=2147483647` | 400 | 500 | FALHOU |
| [NEG-183](#neg-183) | Paginacao normaliza page -1 | `GET /api/veiculos?page=-1` | 200 | 200 | PASSOU |
| [NEG-184](#neg-184) | Paginacao normaliza page abc | `GET /api/veiculos?page=abc` | 200 | 200 | PASSOU |
| [NEG-185](#neg-185) | Paginacao normaliza size 0 | `GET /api/veiculos?size=0` | 200 | 200 | PASSOU |
| [NEG-186](#neg-186) | Paginacao normaliza size -1 | `GET /api/veiculos?size=-1` | 200 | 200 | PASSOU |
| [NEG-187](#neg-187) | Paginacao normaliza size abc | `GET /api/veiculos?size=abc` | 200 | 200 | PASSOU |
| [NEG-188](#neg-188) | Paginacao normaliza size 100 | `GET /api/veiculos?size=100` | 200 | 200 | PASSOU |
| [NEG-189](#neg-189) | Pagina fora dos resultados | `GET /api/veiculos?page=100000` | 200 | 200 | PASSOU |
| [NEG-190](#neg-190) | proprietario PATCH vazio preserva dados | `PATCH /api/proprietarios/{{erroProprietarioId}}` | 200 | 200 | PASSOU |
| [NEG-191](#neg-191) | proprietario PATCH campos nulos preserva dados | `PATCH /api/proprietarios/{{erroProprietarioId}}` | 200 | 200 | PASSOU |
| [NEG-192](#neg-192) | proprietario falhas nao alteraram registro | `GET /api/proprietarios/{{erroProprietarioId}}` | 200 | 200 | PASSOU |
| [NEG-193](#neg-193) | veiculo PATCH vazio preserva dados | `PATCH /api/veiculos/{{erroVeiculoId}}` | 200 | 200 | PASSOU |
| [NEG-194](#neg-194) | veiculo PATCH campos nulos preserva dados | `PATCH /api/veiculos/{{erroVeiculoId}}` | 200 | 200 | PASSOU |
| [NEG-195](#neg-195) | veiculo falhas nao alteraram registro | `GET /api/veiculos/{{erroVeiculoId}}` | 200 | 200 | PASSOU |
| [NEG-196](#neg-196) | segundo veiculo conflitos nao alteraram registro | `GET /api/veiculos/{{erroVeiculo2Id}}` | 200 | 200 | PASSOU |
| [NEG-197](#neg-197) | Excluir veiculo de teste | `DELETE /api/veiculos/{{erroVeiculoId}}` | 204 | 204 | PASSOU |
| [NEG-198](#neg-198) | veiculo excluido GET | `GET /api/veiculos/{{erroVeiculoId}}` | 404 | 404 | PASSOU |
| [NEG-199](#neg-199) | veiculo excluido PATCH | `PATCH /api/veiculos/{{erroVeiculoId}}` | 404 | 404 | PASSOU |
| [NEG-200](#neg-200) | veiculo excluido DELETE | `DELETE /api/veiculos/{{erroVeiculoId}}` | 404 | 404 | PASSOU |
| [NEG-201](#neg-201) | Excluir proprietario e veiculo em cascata | `DELETE /api/proprietarios/{{erroProprietarioId}}` | 204 | 204 | PASSOU |
| [NEG-202](#neg-202) | proprietario excluido GET | `GET /api/proprietarios/{{erroProprietarioId}}` | 404 | 404 | PASSOU |
| [NEG-203](#neg-203) | proprietario excluido PATCH | `PATCH /api/proprietarios/{{erroProprietarioId}}` | 404 | 404 | PASSOU |
| [NEG-204](#neg-204) | proprietario excluido DELETE | `DELETE /api/proprietarios/{{erroProprietarioId}}` | 404 | 404 | PASSOU |
| [NEG-205](#neg-205) | veiculo cascata excluido GET | `GET /api/veiculos/{{erroVeiculo2Id}}` | 404 | 404 | PASSOU |
| [NEG-206](#neg-206) | veiculo cascata excluido PATCH | `PATCH /api/veiculos/{{erroVeiculo2Id}}` | 404 | 404 | PASSOU |
| [NEG-207](#neg-207) | veiculo cascata excluido DELETE | `DELETE /api/veiculos/{{erroVeiculo2Id}}` | 404 | 404 | PASSOU |
| [NEG-208](#neg-208) | Cadastrar veiculo com dono inativo | `POST /api/veiculos` | 404 | 404 | PASSOU |
| [NEG-209](#neg-209) | CPF excluido continua reservado | `POST /api/proprietarios` | 409 | 409 | PASSOU |
| [NEG-210](#neg-210) | Listagem oculta proprietario inativo | `GET /api/proprietarios` | 200 | 200 | PASSOU |
| [NEG-211](#neg-211) | Listagem oculta veiculos inativos | `GET /api/veiculos` | 200 | 200 | PASSOU |
| [NEG-212](#neg-212) | Historico preservado apos exclusao | `GET /api/historico` | 200 | 200 | PASSOU |
| [NEG-213](#neg-213) | Log preservado apos exclusao | `GET /api/log-proprietarios` | 200 | 200 | PASSOU |

## Requisições e asserções de cada cenário

Os valores `{{erro...}}` são gerados/capturados pela preparação. Os arquivos `.bru` abaixo contêm o teste executável completo, inclusive scripts e asserções.

### NEG-001

**Preparar proprietario** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/001%20Preparar%20proprietario.bru).

`POST {{baseUrl}}/api/proprietarios` — esperado **201**, recebido **201**.

Corpo enviado:

```json
{"cpfCnpj": "{{erroCpf}}", "nome": "Teste Cenarios Erro", "cep": "60000000", "estado": "CE"}
```

Asserções executadas:

- PASSOU: NEG-001: HTTP 201.

### NEG-002

**Preparar veiculo** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/002%20Preparar%20veiculo.bru).

`POST {{baseUrl}}/api/veiculos` — esperado **201**, recebido **201**.

Corpo enviado:

```json
{"tipo": "CARRO", "idProprietario": {{erroProprietarioId}}, "anoFabricacao": 2024, "chassi": "{{erroChassi}}", "renavam": "{{erroRenavam}}", "placa": "{{erroPlaca}}"}
```

Asserções executadas:

- PASSOU: NEG-002: HTTP 201.

### NEG-003

**Preparar segundo veiculo** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/003%20Preparar%20segundo%20veiculo.bru).

`POST {{baseUrl}}/api/veiculos` — esperado **201**, recebido **201**.

Corpo enviado:

```json
{"tipo": "CARRO", "idProprietario": {{erroProprietarioId}}, "anoFabricacao": 2024, "chassi": "{{erroChassi2}}", "renavam": "{{erroRenavam2}}", "placa": "{{erroPlaca2}}"}
```

Asserções executadas:

- PASSOU: NEG-003: HTTP 201.

### NEG-004

**proprietarios GET ID texto** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/004%20proprietarios%20GET%20ID%20texto.bru).

`GET {{baseUrl}}/api/proprietarios/abc` — esperado **400**, recebido **400**.

Corpo: ausente.

Asserções executadas:

- PASSOU: NEG-004: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-005

**proprietarios GET ID zero** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/005%20proprietarios%20GET%20ID%20zero.bru).

`GET {{baseUrl}}/api/proprietarios/0` — esperado **400**, recebido **400**.

Corpo: ausente.

Asserções executadas:

- PASSOU: NEG-005: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-006

**proprietarios GET ID negativo** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/006%20proprietarios%20GET%20ID%20negativo.bru).

`GET {{baseUrl}}/api/proprietarios/-1` — esperado **400**, recebido **400**.

Corpo: ausente.

Asserções executadas:

- PASSOU: NEG-006: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-007

**proprietarios GET ID decimal** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/007%20proprietarios%20GET%20ID%20decimal.bru).

`GET {{baseUrl}}/api/proprietarios/1.5` — esperado **400**, recebido **400**.

Corpo: ausente.

Asserções executadas:

- PASSOU: NEG-007: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-008

**proprietarios GET ID overflow** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/008%20proprietarios%20GET%20ID%20overflow.bru).

`GET {{baseUrl}}/api/proprietarios/9223372036854775808` — esperado **400**, recebido **400**.

Corpo: ausente.

Asserções executadas:

- PASSOU: NEG-008: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-009

**proprietarios GET ID inexistente** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/009%20proprietarios%20GET%20ID%20inexistente.bru).

`GET {{baseUrl}}/api/proprietarios/2147483647` — esperado **404**, recebido **404**.

Corpo: ausente.

ID positivo reservado para ausência; a execução pressupõe que 2147483647 não exista.

Asserções executadas:

- PASSOU: NEG-009: HTTP 404.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-010

**proprietarios PATCH ID texto** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/010%20proprietarios%20PATCH%20ID%20texto.bru).

`PATCH {{baseUrl}}/api/proprietarios/abc` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{}
```

Asserções executadas:

- PASSOU: NEG-010: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-011

**proprietarios PATCH ID zero** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/011%20proprietarios%20PATCH%20ID%20zero.bru).

`PATCH {{baseUrl}}/api/proprietarios/0` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{}
```

Asserções executadas:

- PASSOU: NEG-011: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-012

**proprietarios PATCH ID negativo** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/012%20proprietarios%20PATCH%20ID%20negativo.bru).

`PATCH {{baseUrl}}/api/proprietarios/-1` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{}
```

Asserções executadas:

- PASSOU: NEG-012: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-013

**proprietarios PATCH ID decimal** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/013%20proprietarios%20PATCH%20ID%20decimal.bru).

`PATCH {{baseUrl}}/api/proprietarios/1.5` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{}
```

Asserções executadas:

- PASSOU: NEG-013: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-014

**proprietarios PATCH ID overflow** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/014%20proprietarios%20PATCH%20ID%20overflow.bru).

`PATCH {{baseUrl}}/api/proprietarios/9223372036854775808` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{}
```

Asserções executadas:

- PASSOU: NEG-014: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-015

**proprietarios PATCH ID inexistente** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/015%20proprietarios%20PATCH%20ID%20inexistente.bru).

`PATCH {{baseUrl}}/api/proprietarios/2147483647` — esperado **404**, recebido **404**.

Corpo enviado:

```json
{}
```

ID positivo reservado para ausência; a execução pressupõe que 2147483647 não exista.

Asserções executadas:

- PASSOU: NEG-015: HTTP 404.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-016

**proprietarios DELETE ID texto** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/016%20proprietarios%20DELETE%20ID%20texto.bru).

`DELETE {{baseUrl}}/api/proprietarios/abc` — esperado **400**, recebido **400**.

Corpo: ausente.

Asserções executadas:

- PASSOU: NEG-016: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-017

**proprietarios DELETE ID zero** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/017%20proprietarios%20DELETE%20ID%20zero.bru).

`DELETE {{baseUrl}}/api/proprietarios/0` — esperado **400**, recebido **400**.

Corpo: ausente.

Asserções executadas:

- PASSOU: NEG-017: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-018

**proprietarios DELETE ID negativo** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/018%20proprietarios%20DELETE%20ID%20negativo.bru).

`DELETE {{baseUrl}}/api/proprietarios/-1` — esperado **400**, recebido **400**.

Corpo: ausente.

Asserções executadas:

- PASSOU: NEG-018: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-019

**proprietarios DELETE ID decimal** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/019%20proprietarios%20DELETE%20ID%20decimal.bru).

`DELETE {{baseUrl}}/api/proprietarios/1.5` — esperado **400**, recebido **400**.

Corpo: ausente.

Asserções executadas:

- PASSOU: NEG-019: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-020

**proprietarios DELETE ID overflow** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/020%20proprietarios%20DELETE%20ID%20overflow.bru).

`DELETE {{baseUrl}}/api/proprietarios/9223372036854775808` — esperado **400**, recebido **400**.

Corpo: ausente.

Asserções executadas:

- PASSOU: NEG-020: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-021

**proprietarios DELETE ID inexistente** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/021%20proprietarios%20DELETE%20ID%20inexistente.bru).

`DELETE {{baseUrl}}/api/proprietarios/2147483647` — esperado **404**, recebido **404**.

Corpo: ausente.

ID positivo reservado para ausência; a execução pressupõe que 2147483647 não exista.

Asserções executadas:

- PASSOU: NEG-021: HTTP 404.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-022

**veiculos GET ID texto** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/022%20veiculos%20GET%20ID%20texto.bru).

`GET {{baseUrl}}/api/veiculos/abc` — esperado **400**, recebido **400**.

Corpo: ausente.

Asserções executadas:

- PASSOU: NEG-022: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-023

**veiculos GET ID zero** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/023%20veiculos%20GET%20ID%20zero.bru).

`GET {{baseUrl}}/api/veiculos/0` — esperado **400**, recebido **400**.

Corpo: ausente.

Asserções executadas:

- PASSOU: NEG-023: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-024

**veiculos GET ID negativo** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/024%20veiculos%20GET%20ID%20negativo.bru).

`GET {{baseUrl}}/api/veiculos/-1` — esperado **400**, recebido **400**.

Corpo: ausente.

Asserções executadas:

- PASSOU: NEG-024: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-025

**veiculos GET ID decimal** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/025%20veiculos%20GET%20ID%20decimal.bru).

`GET {{baseUrl}}/api/veiculos/1.5` — esperado **400**, recebido **400**.

Corpo: ausente.

Asserções executadas:

- PASSOU: NEG-025: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-026

**veiculos GET ID overflow** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/026%20veiculos%20GET%20ID%20overflow.bru).

`GET {{baseUrl}}/api/veiculos/9223372036854775808` — esperado **400**, recebido **400**.

Corpo: ausente.

Asserções executadas:

- PASSOU: NEG-026: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-027

**veiculos GET ID inexistente** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/027%20veiculos%20GET%20ID%20inexistente.bru).

`GET {{baseUrl}}/api/veiculos/2147483647` — esperado **404**, recebido **404**.

Corpo: ausente.

ID positivo reservado para ausência; a execução pressupõe que 2147483647 não exista.

Asserções executadas:

- PASSOU: NEG-027: HTTP 404.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-028

**veiculos PATCH ID texto** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/028%20veiculos%20PATCH%20ID%20texto.bru).

`PATCH {{baseUrl}}/api/veiculos/abc` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{}
```

Asserções executadas:

- PASSOU: NEG-028: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-029

**veiculos PATCH ID zero** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/029%20veiculos%20PATCH%20ID%20zero.bru).

`PATCH {{baseUrl}}/api/veiculos/0` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{}
```

Asserções executadas:

- PASSOU: NEG-029: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-030

**veiculos PATCH ID negativo** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/030%20veiculos%20PATCH%20ID%20negativo.bru).

`PATCH {{baseUrl}}/api/veiculos/-1` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{}
```

Asserções executadas:

- PASSOU: NEG-030: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-031

**veiculos PATCH ID decimal** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/031%20veiculos%20PATCH%20ID%20decimal.bru).

`PATCH {{baseUrl}}/api/veiculos/1.5` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{}
```

Asserções executadas:

- PASSOU: NEG-031: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-032

**veiculos PATCH ID overflow** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/032%20veiculos%20PATCH%20ID%20overflow.bru).

`PATCH {{baseUrl}}/api/veiculos/9223372036854775808` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{}
```

Asserções executadas:

- PASSOU: NEG-032: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-033

**veiculos PATCH ID inexistente** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/033%20veiculos%20PATCH%20ID%20inexistente.bru).

`PATCH {{baseUrl}}/api/veiculos/2147483647` — esperado **404**, recebido **404**.

Corpo enviado:

```json
{}
```

ID positivo reservado para ausência; a execução pressupõe que 2147483647 não exista.

Asserções executadas:

- PASSOU: NEG-033: HTTP 404.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-034

**veiculos DELETE ID texto** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/034%20veiculos%20DELETE%20ID%20texto.bru).

`DELETE {{baseUrl}}/api/veiculos/abc` — esperado **400**, recebido **400**.

Corpo: ausente.

Asserções executadas:

- PASSOU: NEG-034: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-035

**veiculos DELETE ID zero** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/035%20veiculos%20DELETE%20ID%20zero.bru).

`DELETE {{baseUrl}}/api/veiculos/0` — esperado **400**, recebido **400**.

Corpo: ausente.

Asserções executadas:

- PASSOU: NEG-035: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-036

**veiculos DELETE ID negativo** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/036%20veiculos%20DELETE%20ID%20negativo.bru).

`DELETE {{baseUrl}}/api/veiculos/-1` — esperado **400**, recebido **400**.

Corpo: ausente.

Asserções executadas:

- PASSOU: NEG-036: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-037

**veiculos DELETE ID decimal** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/037%20veiculos%20DELETE%20ID%20decimal.bru).

`DELETE {{baseUrl}}/api/veiculos/1.5` — esperado **400**, recebido **400**.

Corpo: ausente.

Asserções executadas:

- PASSOU: NEG-037: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-038

**veiculos DELETE ID overflow** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/038%20veiculos%20DELETE%20ID%20overflow.bru).

`DELETE {{baseUrl}}/api/veiculos/9223372036854775808` — esperado **400**, recebido **400**.

Corpo: ausente.

Asserções executadas:

- PASSOU: NEG-038: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-039

**veiculos DELETE ID inexistente** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/039%20veiculos%20DELETE%20ID%20inexistente.bru).

`DELETE {{baseUrl}}/api/veiculos/2147483647` — esperado **404**, recebido **404**.

Corpo: ausente.

ID positivo reservado para ausência; a execução pressupõe que 2147483647 não exista.

Asserções executadas:

- PASSOU: NEG-039: HTTP 404.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-040

**historico GET ID texto** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/040%20historico%20GET%20ID%20texto.bru).

`GET {{baseUrl}}/api/historico/abc` — esperado **400**, recebido **400**.

Corpo: ausente.

Asserções executadas:

- PASSOU: NEG-040: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-041

**historico GET ID zero** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/041%20historico%20GET%20ID%20zero.bru).

`GET {{baseUrl}}/api/historico/0` — esperado **400**, recebido **400**.

Corpo: ausente.

Asserções executadas:

- PASSOU: NEG-041: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-042

**historico GET ID negativo** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/042%20historico%20GET%20ID%20negativo.bru).

`GET {{baseUrl}}/api/historico/-1` — esperado **400**, recebido **400**.

Corpo: ausente.

Asserções executadas:

- PASSOU: NEG-042: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-043

**historico GET ID decimal** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/043%20historico%20GET%20ID%20decimal.bru).

`GET {{baseUrl}}/api/historico/1.5` — esperado **400**, recebido **400**.

Corpo: ausente.

Asserções executadas:

- PASSOU: NEG-043: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-044

**historico GET ID overflow** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/044%20historico%20GET%20ID%20overflow.bru).

`GET {{baseUrl}}/api/historico/9223372036854775808` — esperado **400**, recebido **400**.

Corpo: ausente.

Asserções executadas:

- PASSOU: NEG-044: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-045

**historico GET ID inexistente** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/045%20historico%20GET%20ID%20inexistente.bru).

`GET {{baseUrl}}/api/historico/2147483647` — esperado **404**, recebido **404**.

Corpo: ausente.

ID positivo reservado para ausência; a execução pressupõe que 2147483647 não exista.

Asserções executadas:

- PASSOU: NEG-045: HTTP 404.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-046

**log-proprietarios GET ID texto** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/046%20log-proprietarios%20GET%20ID%20texto.bru).

`GET {{baseUrl}}/api/log-proprietarios/abc` — esperado **400**, recebido **400**.

Corpo: ausente.

Asserções executadas:

- PASSOU: NEG-046: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-047

**log-proprietarios GET ID zero** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/047%20log-proprietarios%20GET%20ID%20zero.bru).

`GET {{baseUrl}}/api/log-proprietarios/0` — esperado **400**, recebido **400**.

Corpo: ausente.

Asserções executadas:

- PASSOU: NEG-047: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-048

**log-proprietarios GET ID negativo** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/048%20log-proprietarios%20GET%20ID%20negativo.bru).

`GET {{baseUrl}}/api/log-proprietarios/-1` — esperado **400**, recebido **400**.

Corpo: ausente.

Asserções executadas:

- PASSOU: NEG-048: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-049

**log-proprietarios GET ID decimal** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/049%20log-proprietarios%20GET%20ID%20decimal.bru).

`GET {{baseUrl}}/api/log-proprietarios/1.5` — esperado **400**, recebido **400**.

Corpo: ausente.

Asserções executadas:

- PASSOU: NEG-049: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-050

**log-proprietarios GET ID overflow** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/050%20log-proprietarios%20GET%20ID%20overflow.bru).

`GET {{baseUrl}}/api/log-proprietarios/9223372036854775808` — esperado **400**, recebido **400**.

Corpo: ausente.

Asserções executadas:

- PASSOU: NEG-050: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-051

**log-proprietarios GET ID inexistente** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/051%20log-proprietarios%20GET%20ID%20inexistente.bru).

`GET {{baseUrl}}/api/log-proprietarios/2147483647` — esperado **404**, recebido **404**.

Corpo: ausente.

ID positivo reservado para ausência; a execução pressupõe que 2147483647 não exista.

Asserções executadas:

- PASSOU: NEG-051: HTTP 404.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-052

**proprietario POST cpfCnpj nulo** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/052%20proprietario%20POST%20cpfCnpj%20nulo.bru).

`POST {{baseUrl}}/api/proprietarios` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"cpfCnpj": null, "nome": "Teste Cenarios Erro", "cep": "60000000", "estado": "CE"}
```

Asserções executadas:

- PASSOU: NEG-052: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.
- PASSOU: Campo rejeitado: cpfCnpj.

### NEG-053

**proprietario POST cpfCnpj vazio** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/053%20proprietario%20POST%20cpfCnpj%20vazio.bru).

`POST {{baseUrl}}/api/proprietarios` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"cpfCnpj": "", "nome": "Teste Cenarios Erro", "cep": "60000000", "estado": "CE"}
```

Asserções executadas:

- PASSOU: NEG-053: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.
- PASSOU: Campo rejeitado: cpfCnpj.

### NEG-054

**proprietario POST cpfCnpj branco** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/054%20proprietario%20POST%20cpfCnpj%20branco.bru).

`POST {{baseUrl}}/api/proprietarios` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"cpfCnpj": "   ", "nome": "Teste Cenarios Erro", "cep": "60000000", "estado": "CE"}
```

Asserções executadas:

- PASSOU: NEG-054: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.
- PASSOU: Campo rejeitado: cpfCnpj.

### NEG-055

**proprietario POST cpfCnpj ausente** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/055%20proprietario%20POST%20cpfCnpj%20ausente.bru).

`POST {{baseUrl}}/api/proprietarios` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"nome": "Teste Cenarios Erro", "cep": "60000000", "estado": "CE"}
```

Asserções executadas:

- PASSOU: NEG-055: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.
- PASSOU: Campo rejeitado: cpfCnpj.

### NEG-056

**proprietario POST nome nulo** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/056%20proprietario%20POST%20nome%20nulo.bru).

`POST {{baseUrl}}/api/proprietarios` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"cpfCnpj": "{{erroCpf}}", "nome": null, "cep": "60000000", "estado": "CE"}
```

Asserções executadas:

- PASSOU: NEG-056: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.
- PASSOU: Campo rejeitado: nome.

### NEG-057

**proprietario POST nome vazio** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/057%20proprietario%20POST%20nome%20vazio.bru).

`POST {{baseUrl}}/api/proprietarios` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"cpfCnpj": "{{erroCpf}}", "nome": "", "cep": "60000000", "estado": "CE"}
```

Asserções executadas:

- PASSOU: NEG-057: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.
- PASSOU: Campo rejeitado: nome.

### NEG-058

**proprietario POST nome branco** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/058%20proprietario%20POST%20nome%20branco.bru).

`POST {{baseUrl}}/api/proprietarios` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"cpfCnpj": "{{erroCpf}}", "nome": "   ", "cep": "60000000", "estado": "CE"}
```

Asserções executadas:

- PASSOU: NEG-058: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.
- PASSOU: Campo rejeitado: nome.

### NEG-059

**proprietario POST nome ausente** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/059%20proprietario%20POST%20nome%20ausente.bru).

`POST {{baseUrl}}/api/proprietarios` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"cpfCnpj": "{{erroCpf}}", "cep": "60000000", "estado": "CE"}
```

Asserções executadas:

- PASSOU: NEG-059: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.
- PASSOU: Campo rejeitado: nome.

### NEG-060

**proprietario POST cpfCnpj repetido** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/060%20proprietario%20POST%20cpfCnpj%20repetido.bru).

`POST {{baseUrl}}/api/proprietarios` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"cpfCnpj": "11111111111", "nome": "Teste Cenarios Erro", "cep": "60000000", "estado": "CE"}
```

Asserções executadas:

- PASSOU: NEG-060: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.
- PASSOU: Campo rejeitado: cpfCnpj.

### NEG-061

**proprietario POST cpfCnpj digito invalido** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/061%20proprietario%20POST%20cpfCnpj%20digito%20invalido.bru).

`POST {{baseUrl}}/api/proprietarios` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"cpfCnpj": "12345678900", "nome": "Teste Cenarios Erro", "cep": "60000000", "estado": "CE"}
```

Asserções executadas:

- PASSOU: NEG-061: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.
- PASSOU: Campo rejeitado: cpfCnpj.

### NEG-062

**proprietario POST cpfCnpj CNPJ invalido** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/062%20proprietario%20POST%20cpfCnpj%20CNPJ%20invalido.bru).

`POST {{baseUrl}}/api/proprietarios` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"cpfCnpj": "11222333000100", "nome": "Teste Cenarios Erro", "cep": "60000000", "estado": "CE"}
```

Asserções executadas:

- PASSOU: NEG-062: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.
- PASSOU: Campo rejeitado: cpfCnpj.

### NEG-063

**proprietario POST cep curto** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/063%20proprietario%20POST%20cep%20curto.bru).

`POST {{baseUrl}}/api/proprietarios` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"cpfCnpj": "{{erroCpf}}", "nome": "Teste Cenarios Erro", "cep": "1234", "estado": "CE"}
```

Asserções executadas:

- PASSOU: NEG-063: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.
- PASSOU: Campo rejeitado: cep.

### NEG-064

**proprietario POST cep letras** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/064%20proprietario%20POST%20cep%20letras.bru).

`POST {{baseUrl}}/api/proprietarios` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"cpfCnpj": "{{erroCpf}}", "nome": "Teste Cenarios Erro", "cep": "ABCDEFGH", "estado": "CE"}
```

Asserções executadas:

- PASSOU: NEG-064: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.
- PASSOU: Campo rejeitado: cep.

### NEG-065

**proprietario POST estado longo** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/065%20proprietario%20POST%20estado%20longo.bru).

`POST {{baseUrl}}/api/proprietarios` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"cpfCnpj": "{{erroCpf}}", "nome": "Teste Cenarios Erro", "cep": "60000000", "estado": "CEA"}
```

Asserções executadas:

- PASSOU: NEG-065: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.
- PASSOU: Campo rejeitado: estado.

### NEG-066

**proprietario POST estado numero** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/066%20proprietario%20POST%20estado%20numero.bru).

`POST {{baseUrl}}/api/proprietarios` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"cpfCnpj": "{{erroCpf}}", "nome": "Teste Cenarios Erro", "cep": "60000000", "estado": "1A"}
```

Asserções executadas:

- PASSOU: NEG-066: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.
- PASSOU: Campo rejeitado: estado.

### NEG-067

**proprietario POST nome excedido** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/067%20proprietario%20POST%20nome%20excedido.bru).

`POST {{baseUrl}}/api/proprietarios` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"cpfCnpj": "{{erroCpf}}", "nome": "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "cep": "60000000", "estado": "CE"}
```

Asserções executadas:

- PASSOU: NEG-067: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.
- PASSOU: Campo rejeitado: nome.

### NEG-068

**proprietario POST rua excedido** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/068%20proprietario%20POST%20rua%20excedido.bru).

`POST {{baseUrl}}/api/proprietarios` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"cpfCnpj": "{{erroCpf}}", "nome": "Teste Cenarios Erro", "cep": "60000000", "estado": "CE", "rua": "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"}
```

Asserções executadas:

- PASSOU: NEG-068: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.
- PASSOU: Campo rejeitado: rua.

### NEG-069

**proprietario POST numero excedido** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/069%20proprietario%20POST%20numero%20excedido.bru).

`POST {{baseUrl}}/api/proprietarios` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"cpfCnpj": "{{erroCpf}}", "nome": "Teste Cenarios Erro", "cep": "60000000", "estado": "CE", "numero": "AAAAAAAAAAAAAAAAAAAAA"}
```

Asserções executadas:

- PASSOU: NEG-069: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.
- PASSOU: Campo rejeitado: numero.

### NEG-070

**proprietario POST cidade excedido** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/070%20proprietario%20POST%20cidade%20excedido.bru).

`POST {{baseUrl}}/api/proprietarios` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"cpfCnpj": "{{erroCpf}}", "nome": "Teste Cenarios Erro", "cep": "60000000", "estado": "CE", "cidade": "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"}
```

Asserções executadas:

- PASSOU: NEG-070: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.
- PASSOU: Campo rejeitado: cidade.

### NEG-071

**proprietario POST complemento excedido** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/071%20proprietario%20POST%20complemento%20excedido.bru).

`POST {{baseUrl}}/api/proprietarios` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"cpfCnpj": "{{erroCpf}}", "nome": "Teste Cenarios Erro", "cep": "60000000", "estado": "CE", "complemento": "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"}
```

Asserções executadas:

- PASSOU: NEG-071: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.
- PASSOU: Campo rejeitado: complemento.

### NEG-072

**proprietario PATCH cpfCnpj repetido** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/072%20proprietario%20PATCH%20cpfCnpj%20repetido.bru).

`PATCH {{baseUrl}}/api/proprietarios/{{erroProprietarioId}}` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"cpfCnpj": "11111111111"}
```

Asserções executadas:

- FALHOU: Campo rejeitado: cpfCnpj.
- PASSOU: NEG-072: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

Resposta recebida:

```json
{
  "dataHora": "23/10/2026 11:32:28",
  "status": 400,
  "excecao": "HandlerMethodValidationException",
  "mensagem": "Validation failure",
  "erros": []
}
```

### NEG-073

**proprietario PATCH cpfCnpj digito invalido** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/073%20proprietario%20PATCH%20cpfCnpj%20digito%20invalido.bru).

`PATCH {{baseUrl}}/api/proprietarios/{{erroProprietarioId}}` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"cpfCnpj": "12345678900"}
```

Asserções executadas:

- FALHOU: Campo rejeitado: cpfCnpj.
- PASSOU: NEG-073: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

Resposta recebida:

```json
{
  "dataHora": "23/10/2026 11:32:28",
  "status": 400,
  "excecao": "HandlerMethodValidationException",
  "mensagem": "Validation failure",
  "erros": []
}
```

### NEG-074

**proprietario PATCH cpfCnpj CNPJ invalido** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/074%20proprietario%20PATCH%20cpfCnpj%20CNPJ%20invalido.bru).

`PATCH {{baseUrl}}/api/proprietarios/{{erroProprietarioId}}` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"cpfCnpj": "11222333000100"}
```

Asserções executadas:

- FALHOU: Campo rejeitado: cpfCnpj.
- PASSOU: NEG-074: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

Resposta recebida:

```json
{
  "dataHora": "23/10/2026 11:32:28",
  "status": 400,
  "excecao": "HandlerMethodValidationException",
  "mensagem": "Validation failure",
  "erros": []
}
```

### NEG-075

**proprietario PATCH cep curto** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/075%20proprietario%20PATCH%20cep%20curto.bru).

`PATCH {{baseUrl}}/api/proprietarios/{{erroProprietarioId}}` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"cep": "1234"}
```

Asserções executadas:

- FALHOU: Campo rejeitado: cep.
- PASSOU: NEG-075: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

Resposta recebida:

```json
{
  "dataHora": "23/10/2026 11:32:28",
  "status": 400,
  "excecao": "HandlerMethodValidationException",
  "mensagem": "Validation failure",
  "erros": []
}
```

### NEG-076

**proprietario PATCH cep letras** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/076%20proprietario%20PATCH%20cep%20letras.bru).

`PATCH {{baseUrl}}/api/proprietarios/{{erroProprietarioId}}` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"cep": "ABCDEFGH"}
```

Asserções executadas:

- FALHOU: Campo rejeitado: cep.
- PASSOU: NEG-076: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

Resposta recebida:

```json
{
  "dataHora": "23/10/2026 11:32:28",
  "status": 400,
  "excecao": "HandlerMethodValidationException",
  "mensagem": "Validation failure",
  "erros": []
}
```

### NEG-077

**proprietario PATCH estado longo** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/077%20proprietario%20PATCH%20estado%20longo.bru).

`PATCH {{baseUrl}}/api/proprietarios/{{erroProprietarioId}}` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"estado": "CEA"}
```

Asserções executadas:

- FALHOU: Campo rejeitado: estado.
- PASSOU: NEG-077: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

Resposta recebida:

```json
{
  "dataHora": "23/10/2026 11:32:28",
  "status": 400,
  "excecao": "HandlerMethodValidationException",
  "mensagem": "Validation failure",
  "erros": []
}
```

### NEG-078

**proprietario PATCH estado numero** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/078%20proprietario%20PATCH%20estado%20numero.bru).

`PATCH {{baseUrl}}/api/proprietarios/{{erroProprietarioId}}` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"estado": "1A"}
```

Asserções executadas:

- FALHOU: Campo rejeitado: estado.
- PASSOU: NEG-078: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

Resposta recebida:

```json
{
  "dataHora": "23/10/2026 11:32:28",
  "status": 400,
  "excecao": "HandlerMethodValidationException",
  "mensagem": "Validation failure",
  "erros": []
}
```

### NEG-079

**proprietario PATCH nome excedido** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/079%20proprietario%20PATCH%20nome%20excedido.bru).

`PATCH {{baseUrl}}/api/proprietarios/{{erroProprietarioId}}` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"nome": "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"}
```

Asserções executadas:

- FALHOU: Campo rejeitado: nome.
- PASSOU: NEG-079: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

Resposta recebida:

```json
{
  "dataHora": "23/10/2026 11:32:28",
  "status": 400,
  "excecao": "HandlerMethodValidationException",
  "mensagem": "Validation failure",
  "erros": []
}
```

### NEG-080

**proprietario PATCH rua excedido** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/080%20proprietario%20PATCH%20rua%20excedido.bru).

`PATCH {{baseUrl}}/api/proprietarios/{{erroProprietarioId}}` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"rua": "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"}
```

Asserções executadas:

- FALHOU: Campo rejeitado: rua.
- PASSOU: NEG-080: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

Resposta recebida:

```json
{
  "dataHora": "23/10/2026 11:32:28",
  "status": 400,
  "excecao": "HandlerMethodValidationException",
  "mensagem": "Validation failure",
  "erros": []
}
```

### NEG-081

**proprietario PATCH numero excedido** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/081%20proprietario%20PATCH%20numero%20excedido.bru).

`PATCH {{baseUrl}}/api/proprietarios/{{erroProprietarioId}}` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"numero": "AAAAAAAAAAAAAAAAAAAAA"}
```

Asserções executadas:

- FALHOU: Campo rejeitado: numero.
- PASSOU: NEG-081: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

Resposta recebida:

```json
{
  "dataHora": "23/10/2026 11:32:28",
  "status": 400,
  "excecao": "HandlerMethodValidationException",
  "mensagem": "Validation failure",
  "erros": []
}
```

### NEG-082

**proprietario PATCH cidade excedido** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/082%20proprietario%20PATCH%20cidade%20excedido.bru).

`PATCH {{baseUrl}}/api/proprietarios/{{erroProprietarioId}}` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"cidade": "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"}
```

Asserções executadas:

- FALHOU: Campo rejeitado: cidade.
- PASSOU: NEG-082: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

Resposta recebida:

```json
{
  "dataHora": "23/10/2026 11:32:28",
  "status": 400,
  "excecao": "HandlerMethodValidationException",
  "mensagem": "Validation failure",
  "erros": []
}
```

### NEG-083

**proprietario PATCH complemento excedido** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/083%20proprietario%20PATCH%20complemento%20excedido.bru).

`PATCH {{baseUrl}}/api/proprietarios/{{erroProprietarioId}}` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"complemento": "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"}
```

Asserções executadas:

- FALHOU: Campo rejeitado: complemento.
- PASSOU: NEG-083: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

Resposta recebida:

```json
{
  "dataHora": "23/10/2026 11:32:28",
  "status": 400,
  "excecao": "HandlerMethodValidationException",
  "mensagem": "Validation failure",
  "erros": []
}
```

### NEG-084

**proprietario PATCH nome branco** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/084%20proprietario%20PATCH%20nome%20branco.bru).

`PATCH {{baseUrl}}/api/proprietarios/{{erroProprietarioId}}` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"nome": "   "}
```

Asserções executadas:

- PASSOU: NEG-084: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-085

**proprietario PATCH rua branco** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/085%20proprietario%20PATCH%20rua%20branco.bru).

`PATCH {{baseUrl}}/api/proprietarios/{{erroProprietarioId}}` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"rua": "   "}
```

Asserções executadas:

- PASSOU: NEG-085: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-086

**proprietario PATCH numero branco** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/086%20proprietario%20PATCH%20numero%20branco.bru).

`PATCH {{baseUrl}}/api/proprietarios/{{erroProprietarioId}}` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"numero": "   "}
```

Asserções executadas:

- PASSOU: NEG-086: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-087

**proprietario PATCH cep branco** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/087%20proprietario%20PATCH%20cep%20branco.bru).

`PATCH {{baseUrl}}/api/proprietarios/{{erroProprietarioId}}` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"cep": "   "}
```

Asserções executadas:

- PASSOU: NEG-087: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-088

**proprietario PATCH cidade branco** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/088%20proprietario%20PATCH%20cidade%20branco.bru).

`PATCH {{baseUrl}}/api/proprietarios/{{erroProprietarioId}}` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"cidade": "   "}
```

Asserções executadas:

- PASSOU: NEG-088: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-089

**proprietario PATCH estado branco** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/089%20proprietario%20PATCH%20estado%20branco.bru).

`PATCH {{baseUrl}}/api/proprietarios/{{erroProprietarioId}}` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"estado": "   "}
```

Asserções executadas:

- PASSOU: NEG-089: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-090

**proprietario PATCH complemento branco** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/090%20proprietario%20PATCH%20complemento%20branco.bru).

`PATCH {{baseUrl}}/api/proprietarios/{{erroProprietarioId}}` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"complemento": "   "}
```

Asserções executadas:

- PASSOU: NEG-090: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-091

**proprietario PATCH cpfCnpj branco** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/091%20proprietario%20PATCH%20cpfCnpj%20branco.bru).

`PATCH {{baseUrl}}/api/proprietarios/{{erroProprietarioId}}` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"cpfCnpj": "   "}
```

Asserções executadas:

- PASSOU: NEG-091: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-092

**proprietario CPF duplicado** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/092%20proprietario%20CPF%20duplicado.bru).

`POST {{baseUrl}}/api/proprietarios` — esperado **409**, recebido **409**.

Corpo enviado:

```json
{"cpfCnpj": "{{erroCpf}}", "nome": "Teste Cenarios Erro", "cep": "60000000", "estado": "CE"}
```

Asserções executadas:

- PASSOU: NEG-092: HTTP 409.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-093

**proprietario CPF imutavel** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/093%20proprietario%20CPF%20imutavel.bru).

`PATCH {{baseUrl}}/api/proprietarios/{{erroProprietarioId}}` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"cpfCnpj": "{{erroOutroCpf}}"}
```

Asserções executadas:

- PASSOU: NEG-093: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-094

**veiculo POST tipo nulo** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/094%20veiculo%20POST%20tipo%20nulo.bru).

`POST {{baseUrl}}/api/veiculos` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"tipo": null, "idProprietario": {{erroProprietarioId}}, "anoFabricacao": 2024, "chassi": "{{erroChassi}}", "renavam": "{{erroRenavam}}", "placa": "{{erroPlaca}}"}
```

Asserções executadas:

- PASSOU: NEG-094: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.
- PASSOU: Campo rejeitado: tipo.

### NEG-095

**veiculo POST tipo ausente** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/095%20veiculo%20POST%20tipo%20ausente.bru).

`POST {{baseUrl}}/api/veiculos` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"idProprietario": {{erroProprietarioId}}, "anoFabricacao": 2024, "chassi": "{{erroChassi}}", "renavam": "{{erroRenavam}}", "placa": "{{erroPlaca}}"}
```

Asserções executadas:

- PASSOU: NEG-095: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.
- PASSOU: Campo rejeitado: tipo.

### NEG-096

**veiculo POST tipo vazio** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/096%20veiculo%20POST%20tipo%20vazio.bru).

`POST {{baseUrl}}/api/veiculos` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"tipo": "", "idProprietario": {{erroProprietarioId}}, "anoFabricacao": 2024, "chassi": "{{erroChassi}}", "renavam": "{{erroRenavam}}", "placa": "{{erroPlaca}}"}
```

Asserções executadas:

- PASSOU: NEG-096: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.
- PASSOU: Campo rejeitado: tipo.

### NEG-097

**veiculo POST tipo branco** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/097%20veiculo%20POST%20tipo%20branco.bru).

`POST {{baseUrl}}/api/veiculos` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"tipo": "   ", "idProprietario": {{erroProprietarioId}}, "anoFabricacao": 2024, "chassi": "{{erroChassi}}", "renavam": "{{erroRenavam}}", "placa": "{{erroPlaca}}"}
```

Asserções executadas:

- PASSOU: NEG-097: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.
- PASSOU: Campo rejeitado: tipo.

### NEG-098

**veiculo POST idProprietario nulo** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/098%20veiculo%20POST%20idProprietario%20nulo.bru).

`POST {{baseUrl}}/api/veiculos` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"tipo": "CARRO", "idProprietario": null, "anoFabricacao": 2024, "chassi": "{{erroChassi}}", "renavam": "{{erroRenavam}}", "placa": "{{erroPlaca}}"}
```

Asserções executadas:

- PASSOU: NEG-098: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.
- PASSOU: Campo rejeitado: idProprietario.

### NEG-099

**veiculo POST idProprietario ausente** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/099%20veiculo%20POST%20idProprietario%20ausente.bru).

`POST {{baseUrl}}/api/veiculos` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"tipo": "CARRO", "anoFabricacao": 2024, "chassi": "{{erroChassi}}", "renavam": "{{erroRenavam}}", "placa": "{{erroPlaca}}"}
```

Asserções executadas:

- PASSOU: NEG-099: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.
- PASSOU: Campo rejeitado: idProprietario.

### NEG-100

**veiculo POST anoFabricacao nulo** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/100%20veiculo%20POST%20anoFabricacao%20nulo.bru).

`POST {{baseUrl}}/api/veiculos` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"tipo": "CARRO", "idProprietario": {{erroProprietarioId}}, "anoFabricacao": null, "chassi": "{{erroChassi}}", "renavam": "{{erroRenavam}}", "placa": "{{erroPlaca}}"}
```

Asserções executadas:

- PASSOU: NEG-100: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.
- PASSOU: Campo rejeitado: anoFabricacao.

### NEG-101

**veiculo POST anoFabricacao ausente** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/101%20veiculo%20POST%20anoFabricacao%20ausente.bru).

`POST {{baseUrl}}/api/veiculos` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"tipo": "CARRO", "idProprietario": {{erroProprietarioId}}, "chassi": "{{erroChassi}}", "renavam": "{{erroRenavam}}", "placa": "{{erroPlaca}}"}
```

Asserções executadas:

- PASSOU: NEG-101: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.
- PASSOU: Campo rejeitado: anoFabricacao.

### NEG-102

**veiculo POST chassi nulo** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/102%20veiculo%20POST%20chassi%20nulo.bru).

`POST {{baseUrl}}/api/veiculos` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"tipo": "CARRO", "idProprietario": {{erroProprietarioId}}, "anoFabricacao": 2024, "chassi": null, "renavam": "{{erroRenavam}}", "placa": "{{erroPlaca}}"}
```

Asserções executadas:

- PASSOU: NEG-102: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.
- PASSOU: Campo rejeitado: chassi.

### NEG-103

**veiculo POST chassi ausente** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/103%20veiculo%20POST%20chassi%20ausente.bru).

`POST {{baseUrl}}/api/veiculos` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"tipo": "CARRO", "idProprietario": {{erroProprietarioId}}, "anoFabricacao": 2024, "renavam": "{{erroRenavam}}", "placa": "{{erroPlaca}}"}
```

Asserções executadas:

- PASSOU: NEG-103: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.
- PASSOU: Campo rejeitado: chassi.

### NEG-104

**veiculo POST chassi vazio** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/104%20veiculo%20POST%20chassi%20vazio.bru).

`POST {{baseUrl}}/api/veiculos` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"tipo": "CARRO", "idProprietario": {{erroProprietarioId}}, "anoFabricacao": 2024, "chassi": "", "renavam": "{{erroRenavam}}", "placa": "{{erroPlaca}}"}
```

Asserções executadas:

- PASSOU: NEG-104: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.
- PASSOU: Campo rejeitado: chassi.

### NEG-105

**veiculo POST chassi branco** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/105%20veiculo%20POST%20chassi%20branco.bru).

`POST {{baseUrl}}/api/veiculos` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"tipo": "CARRO", "idProprietario": {{erroProprietarioId}}, "anoFabricacao": 2024, "chassi": "   ", "renavam": "{{erroRenavam}}", "placa": "{{erroPlaca}}"}
```

Asserções executadas:

- PASSOU: NEG-105: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.
- PASSOU: Campo rejeitado: chassi.

### NEG-106

**veiculo POST renavam nulo** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/106%20veiculo%20POST%20renavam%20nulo.bru).

`POST {{baseUrl}}/api/veiculos` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"tipo": "CARRO", "idProprietario": {{erroProprietarioId}}, "anoFabricacao": 2024, "chassi": "{{erroChassi}}", "renavam": null, "placa": "{{erroPlaca}}"}
```

Asserções executadas:

- PASSOU: NEG-106: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.
- PASSOU: Campo rejeitado: renavam.

### NEG-107

**veiculo POST renavam ausente** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/107%20veiculo%20POST%20renavam%20ausente.bru).

`POST {{baseUrl}}/api/veiculos` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"tipo": "CARRO", "idProprietario": {{erroProprietarioId}}, "anoFabricacao": 2024, "chassi": "{{erroChassi}}", "placa": "{{erroPlaca}}"}
```

Asserções executadas:

- PASSOU: NEG-107: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.
- PASSOU: Campo rejeitado: renavam.

### NEG-108

**veiculo POST renavam vazio** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/108%20veiculo%20POST%20renavam%20vazio.bru).

`POST {{baseUrl}}/api/veiculos` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"tipo": "CARRO", "idProprietario": {{erroProprietarioId}}, "anoFabricacao": 2024, "chassi": "{{erroChassi}}", "renavam": "", "placa": "{{erroPlaca}}"}
```

Asserções executadas:

- PASSOU: NEG-108: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.
- PASSOU: Campo rejeitado: renavam.

### NEG-109

**veiculo POST renavam branco** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/109%20veiculo%20POST%20renavam%20branco.bru).

`POST {{baseUrl}}/api/veiculos` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"tipo": "CARRO", "idProprietario": {{erroProprietarioId}}, "anoFabricacao": 2024, "chassi": "{{erroChassi}}", "renavam": "   ", "placa": "{{erroPlaca}}"}
```

Asserções executadas:

- PASSOU: NEG-109: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.
- PASSOU: Campo rejeitado: renavam.

### NEG-110

**veiculo POST placa nulo** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/110%20veiculo%20POST%20placa%20nulo.bru).

`POST {{baseUrl}}/api/veiculos` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"tipo": "CARRO", "idProprietario": {{erroProprietarioId}}, "anoFabricacao": 2024, "chassi": "{{erroChassi}}", "renavam": "{{erroRenavam}}", "placa": null}
```

Asserções executadas:

- PASSOU: NEG-110: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.
- PASSOU: Campo rejeitado: placa.

### NEG-111

**veiculo POST placa ausente** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/111%20veiculo%20POST%20placa%20ausente.bru).

`POST {{baseUrl}}/api/veiculos` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"tipo": "CARRO", "idProprietario": {{erroProprietarioId}}, "anoFabricacao": 2024, "chassi": "{{erroChassi}}", "renavam": "{{erroRenavam}}"}
```

Asserções executadas:

- PASSOU: NEG-111: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.
- PASSOU: Campo rejeitado: placa.

### NEG-112

**veiculo POST placa vazio** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/112%20veiculo%20POST%20placa%20vazio.bru).

`POST {{baseUrl}}/api/veiculos` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"tipo": "CARRO", "idProprietario": {{erroProprietarioId}}, "anoFabricacao": 2024, "chassi": "{{erroChassi}}", "renavam": "{{erroRenavam}}", "placa": ""}
```

Asserções executadas:

- PASSOU: NEG-112: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.
- PASSOU: Campo rejeitado: placa.

### NEG-113

**veiculo POST placa branco** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/113%20veiculo%20POST%20placa%20branco.bru).

`POST {{baseUrl}}/api/veiculos` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"tipo": "CARRO", "idProprietario": {{erroProprietarioId}}, "anoFabricacao": 2024, "chassi": "{{erroChassi}}", "renavam": "{{erroRenavam}}", "placa": "   "}
```

Asserções executadas:

- PASSOU: NEG-113: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.
- PASSOU: Campo rejeitado: placa.

### NEG-114

**veiculo POST tipo invalido** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/114%20veiculo%20POST%20tipo%20invalido.bru).

`POST {{baseUrl}}/api/veiculos` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"tipo": "CAMINHAO", "idProprietario": {{erroProprietarioId}}, "anoFabricacao": 2024, "chassi": "{{erroChassi}}", "renavam": "{{erroRenavam}}", "placa": "{{erroPlaca}}"}
```

Asserções executadas:

- PASSOU: NEG-114: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.
- PASSOU: Campo rejeitado: tipo.

### NEG-115

**veiculo POST idProprietario zero** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/115%20veiculo%20POST%20idProprietario%20zero.bru).

`POST {{baseUrl}}/api/veiculos` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"tipo": "CARRO", "idProprietario": 0, "anoFabricacao": 2024, "chassi": "{{erroChassi}}", "renavam": "{{erroRenavam}}", "placa": "{{erroPlaca}}"}
```

Asserções executadas:

- PASSOU: NEG-115: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.
- PASSOU: Campo rejeitado: idProprietario.

### NEG-116

**veiculo POST idProprietario negativo** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/116%20veiculo%20POST%20idProprietario%20negativo.bru).

`POST {{baseUrl}}/api/veiculos` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"tipo": "CARRO", "idProprietario": -1, "anoFabricacao": 2024, "chassi": "{{erroChassi}}", "renavam": "{{erroRenavam}}", "placa": "{{erroPlaca}}"}
```

Asserções executadas:

- PASSOU: NEG-116: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.
- PASSOU: Campo rejeitado: idProprietario.

### NEG-117

**veiculo POST idProprietario texto** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/117%20veiculo%20POST%20idProprietario%20texto.bru).

`POST {{baseUrl}}/api/veiculos` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"tipo": "CARRO", "idProprietario": "abc", "anoFabricacao": 2024, "chassi": "{{erroChassi}}", "renavam": "{{erroRenavam}}", "placa": "{{erroPlaca}}"}
```

Asserções executadas:

- PASSOU: NEG-117: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-118

**veiculo POST idProprietario overflow** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/118%20veiculo%20POST%20idProprietario%20overflow.bru).

`POST {{baseUrl}}/api/veiculos` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"tipo": "CARRO", "idProprietario": 2147483648, "anoFabricacao": 2024, "chassi": "{{erroChassi}}", "renavam": "{{erroRenavam}}", "placa": "{{erroPlaca}}"}
```

Asserções executadas:

- PASSOU: NEG-118: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-119

**veiculo POST anoFabricacao zero** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/119%20veiculo%20POST%20anoFabricacao%20zero.bru).

`POST {{baseUrl}}/api/veiculos` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"tipo": "CARRO", "idProprietario": {{erroProprietarioId}}, "anoFabricacao": 0, "chassi": "{{erroChassi}}", "renavam": "{{erroRenavam}}", "placa": "{{erroPlaca}}"}
```

Asserções executadas:

- PASSOU: NEG-119: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.
- PASSOU: Campo rejeitado: anoFabricacao.

### NEG-120

**veiculo POST anoFabricacao negativo** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/120%20veiculo%20POST%20anoFabricacao%20negativo.bru).

`POST {{baseUrl}}/api/veiculos` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"tipo": "CARRO", "idProprietario": {{erroProprietarioId}}, "anoFabricacao": -1, "chassi": "{{erroChassi}}", "renavam": "{{erroRenavam}}", "placa": "{{erroPlaca}}"}
```

Asserções executadas:

- PASSOU: NEG-120: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.
- PASSOU: Campo rejeitado: anoFabricacao.

### NEG-121

**veiculo POST anoFabricacao texto** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/121%20veiculo%20POST%20anoFabricacao%20texto.bru).

`POST {{baseUrl}}/api/veiculos` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"tipo": "CARRO", "idProprietario": {{erroProprietarioId}}, "anoFabricacao": "abc", "chassi": "{{erroChassi}}", "renavam": "{{erroRenavam}}", "placa": "{{erroPlaca}}"}
```

Asserções executadas:

- PASSOU: NEG-121: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-122

**veiculo POST anoFabricacao overflow** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/122%20veiculo%20POST%20anoFabricacao%20overflow.bru).

`POST {{baseUrl}}/api/veiculos` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"tipo": "CARRO", "idProprietario": {{erroProprietarioId}}, "anoFabricacao": 2147483648, "chassi": "{{erroChassi}}", "renavam": "{{erroRenavam}}", "placa": "{{erroPlaca}}"}
```

Asserções executadas:

- PASSOU: NEG-122: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-123

**veiculo POST chassi excedido** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/123%20veiculo%20POST%20chassi%20excedido.bru).

`POST {{baseUrl}}/api/veiculos` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"tipo": "CARRO", "idProprietario": {{erroProprietarioId}}, "anoFabricacao": 2024, "chassi": "AAAAAAAAAAAAAAAAAA", "renavam": "{{erroRenavam}}", "placa": "{{erroPlaca}}"}
```

Asserções executadas:

- PASSOU: NEG-123: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.
- PASSOU: Campo rejeitado: chassi.

### NEG-124

**veiculo POST renavam excedido** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/124%20veiculo%20POST%20renavam%20excedido.bru).

`POST {{baseUrl}}/api/veiculos` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"tipo": "CARRO", "idProprietario": {{erroProprietarioId}}, "anoFabricacao": 2024, "chassi": "{{erroChassi}}", "renavam": "AAAAAAAAAAAA", "placa": "{{erroPlaca}}"}
```

Asserções executadas:

- PASSOU: NEG-124: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.
- PASSOU: Campo rejeitado: renavam.

### NEG-125

**veiculo POST placa excedido** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/125%20veiculo%20POST%20placa%20excedido.bru).

`POST {{baseUrl}}/api/veiculos` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"tipo": "CARRO", "idProprietario": {{erroProprietarioId}}, "anoFabricacao": 2024, "chassi": "{{erroChassi}}", "renavam": "{{erroRenavam}}", "placa": "AAAAAAAA"}
```

Asserções executadas:

- PASSOU: NEG-125: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.
- PASSOU: Campo rejeitado: placa.

### NEG-126

**veiculo POST dono inexistente** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/126%20veiculo%20POST%20dono%20inexistente.bru).

`POST {{baseUrl}}/api/veiculos` — esperado **404**, recebido **404**.

Corpo enviado:

```json
{"tipo": "CARRO", "idProprietario": 2147483647, "anoFabricacao": 2024, "chassi": "{{erroChassi}}", "renavam": "{{erroRenavam}}", "placa": "{{erroPlaca}}"}
```

Asserções executadas:

- PASSOU: NEG-126: HTTP 404.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-127

**veiculo PATCH tipo invalido** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/127%20veiculo%20PATCH%20tipo%20invalido.bru).

`PATCH {{baseUrl}}/api/veiculos/{{erroVeiculoId}}` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"tipo": "CAMINHAO"}
```

Asserções executadas:

- FALHOU: Campo rejeitado: tipo.
- PASSOU: NEG-127: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

Resposta recebida:

```json
{
  "dataHora": "23/10/2026 11:32:31",
  "status": 400,
  "excecao": "HandlerMethodValidationException",
  "mensagem": "Validation failure",
  "erros": []
}
```

### NEG-128

**veiculo PATCH idProprietario zero** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/128%20veiculo%20PATCH%20idProprietario%20zero.bru).

`PATCH {{baseUrl}}/api/veiculos/{{erroVeiculoId}}` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"idProprietario": 0}
```

Asserções executadas:

- FALHOU: Campo rejeitado: idProprietario.
- PASSOU: NEG-128: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

Resposta recebida:

```json
{
  "dataHora": "23/10/2026 11:32:31",
  "status": 400,
  "excecao": "HandlerMethodValidationException",
  "mensagem": "Validation failure",
  "erros": []
}
```

### NEG-129

**veiculo PATCH idProprietario negativo** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/129%20veiculo%20PATCH%20idProprietario%20negativo.bru).

`PATCH {{baseUrl}}/api/veiculos/{{erroVeiculoId}}` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"idProprietario": -1}
```

Asserções executadas:

- FALHOU: Campo rejeitado: idProprietario.
- PASSOU: NEG-129: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

Resposta recebida:

```json
{
  "dataHora": "23/10/2026 11:32:31",
  "status": 400,
  "excecao": "HandlerMethodValidationException",
  "mensagem": "Validation failure",
  "erros": []
}
```

### NEG-130

**veiculo PATCH idProprietario texto** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/130%20veiculo%20PATCH%20idProprietario%20texto.bru).

`PATCH {{baseUrl}}/api/veiculos/{{erroVeiculoId}}` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"idProprietario": "abc"}
```

Asserções executadas:

- PASSOU: NEG-130: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-131

**veiculo PATCH idProprietario overflow** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/131%20veiculo%20PATCH%20idProprietario%20overflow.bru).

`PATCH {{baseUrl}}/api/veiculos/{{erroVeiculoId}}` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"idProprietario": 2147483648}
```

Asserções executadas:

- PASSOU: NEG-131: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-132

**veiculo PATCH anoFabricacao zero** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/132%20veiculo%20PATCH%20anoFabricacao%20zero.bru).

`PATCH {{baseUrl}}/api/veiculos/{{erroVeiculoId}}` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"anoFabricacao": 0}
```

Asserções executadas:

- FALHOU: Campo rejeitado: anoFabricacao.
- PASSOU: NEG-132: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

Resposta recebida:

```json
{
  "dataHora": "23/10/2026 11:32:31",
  "status": 400,
  "excecao": "HandlerMethodValidationException",
  "mensagem": "Validation failure",
  "erros": []
}
```

### NEG-133

**veiculo PATCH anoFabricacao negativo** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/133%20veiculo%20PATCH%20anoFabricacao%20negativo.bru).

`PATCH {{baseUrl}}/api/veiculos/{{erroVeiculoId}}` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"anoFabricacao": -1}
```

Asserções executadas:

- FALHOU: Campo rejeitado: anoFabricacao.
- PASSOU: NEG-133: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

Resposta recebida:

```json
{
  "dataHora": "23/10/2026 11:32:32",
  "status": 400,
  "excecao": "HandlerMethodValidationException",
  "mensagem": "Validation failure",
  "erros": []
}
```

### NEG-134

**veiculo PATCH anoFabricacao texto** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/134%20veiculo%20PATCH%20anoFabricacao%20texto.bru).

`PATCH {{baseUrl}}/api/veiculos/{{erroVeiculoId}}` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"anoFabricacao": "abc"}
```

Asserções executadas:

- PASSOU: NEG-134: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-135

**veiculo PATCH anoFabricacao overflow** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/135%20veiculo%20PATCH%20anoFabricacao%20overflow.bru).

`PATCH {{baseUrl}}/api/veiculos/{{erroVeiculoId}}` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"anoFabricacao": 2147483648}
```

Asserções executadas:

- PASSOU: NEG-135: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-136

**veiculo PATCH chassi excedido** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/136%20veiculo%20PATCH%20chassi%20excedido.bru).

`PATCH {{baseUrl}}/api/veiculos/{{erroVeiculoId}}` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"chassi": "AAAAAAAAAAAAAAAAAA"}
```

Asserções executadas:

- FALHOU: Campo rejeitado: chassi.
- PASSOU: NEG-136: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

Resposta recebida:

```json
{
  "dataHora": "23/10/2026 11:32:32",
  "status": 400,
  "excecao": "HandlerMethodValidationException",
  "mensagem": "Validation failure",
  "erros": []
}
```

### NEG-137

**veiculo PATCH renavam excedido** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/137%20veiculo%20PATCH%20renavam%20excedido.bru).

`PATCH {{baseUrl}}/api/veiculos/{{erroVeiculoId}}` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"renavam": "AAAAAAAAAAAA"}
```

Asserções executadas:

- FALHOU: Campo rejeitado: renavam.
- PASSOU: NEG-137: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

Resposta recebida:

```json
{
  "dataHora": "23/10/2026 11:32:32",
  "status": 400,
  "excecao": "HandlerMethodValidationException",
  "mensagem": "Validation failure",
  "erros": []
}
```

### NEG-138

**veiculo PATCH placa excedido** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/138%20veiculo%20PATCH%20placa%20excedido.bru).

`PATCH {{baseUrl}}/api/veiculos/{{erroVeiculoId}}` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"placa": "AAAAAAAA"}
```

Asserções executadas:

- FALHOU: Campo rejeitado: placa.
- PASSOU: NEG-138: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

Resposta recebida:

```json
{
  "dataHora": "23/10/2026 11:32:32",
  "status": 400,
  "excecao": "HandlerMethodValidationException",
  "mensagem": "Validation failure",
  "erros": []
}
```

### NEG-139

**veiculo PATCH dono inexistente** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/139%20veiculo%20PATCH%20dono%20inexistente.bru).

`PATCH {{baseUrl}}/api/veiculos/{{erroVeiculoId}}` — esperado **404**, recebido **404**.

Corpo enviado:

```json
{"idProprietario": 2147483647}
```

Asserções executadas:

- PASSOU: NEG-139: HTTP 404.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-140

**veiculo PATCH tipo branco** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/140%20veiculo%20PATCH%20tipo%20branco.bru).

`PATCH {{baseUrl}}/api/veiculos/{{erroVeiculoId}}` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"tipo": "   "}
```

Asserções executadas:

- PASSOU: NEG-140: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-141

**veiculo PATCH chassi branco** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/141%20veiculo%20PATCH%20chassi%20branco.bru).

`PATCH {{baseUrl}}/api/veiculos/{{erroVeiculoId}}` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"chassi": "   "}
```

Asserções executadas:

- PASSOU: NEG-141: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-142

**veiculo PATCH renavam branco** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/142%20veiculo%20PATCH%20renavam%20branco.bru).

`PATCH {{baseUrl}}/api/veiculos/{{erroVeiculoId}}` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"renavam": "   "}
```

Asserções executadas:

- PASSOU: NEG-142: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-143

**veiculo PATCH placa branco** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/143%20veiculo%20PATCH%20placa%20branco.bru).

`PATCH {{baseUrl}}/api/veiculos/{{erroVeiculoId}}` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"placa": "   "}
```

Asserções executadas:

- PASSOU: NEG-143: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-144

**veiculo POST chassi duplicado** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/144%20veiculo%20POST%20chassi%20duplicado.bru).

`POST {{baseUrl}}/api/veiculos` — esperado **409**, recebido **409**.

Corpo enviado:

```json
{"tipo": "CARRO", "idProprietario": {{erroProprietarioId}}, "anoFabricacao": 2024, "chassi": "{{erroChassi}}", "renavam": "{{erroNovoRenavam}}", "placa": "{{erroNovaPlaca}}"}
```

Asserções executadas:

- PASSOU: NEG-144: HTTP 409.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-145

**veiculo PATCH chassi duplicado** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/145%20veiculo%20PATCH%20chassi%20duplicado.bru).

`PATCH {{baseUrl}}/api/veiculos/{{erroVeiculo2Id}}` — esperado **409**, recebido **409**.

Corpo enviado:

```json
{"chassi": "{{erroChassi}}"}
```

Asserções executadas:

- PASSOU: NEG-145: HTTP 409.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-146

**veiculo POST renavam duplicado** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/146%20veiculo%20POST%20renavam%20duplicado.bru).

`POST {{baseUrl}}/api/veiculos` — esperado **409**, recebido **409**.

Corpo enviado:

```json
{"tipo": "CARRO", "idProprietario": {{erroProprietarioId}}, "anoFabricacao": 2024, "chassi": "{{erroNovoChassi}}", "renavam": "{{erroRenavam}}", "placa": "{{erroNovaPlaca}}"}
```

Asserções executadas:

- PASSOU: NEG-146: HTTP 409.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-147

**veiculo PATCH renavam duplicado** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/147%20veiculo%20PATCH%20renavam%20duplicado.bru).

`PATCH {{baseUrl}}/api/veiculos/{{erroVeiculo2Id}}` — esperado **409**, recebido **409**.

Corpo enviado:

```json
{"renavam": "{{erroRenavam}}"}
```

Asserções executadas:

- PASSOU: NEG-147: HTTP 409.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-148

**veiculo POST placa duplicado** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/148%20veiculo%20POST%20placa%20duplicado.bru).

`POST {{baseUrl}}/api/veiculos` — esperado **409**, recebido **409**.

Corpo enviado:

```json
{"tipo": "CARRO", "idProprietario": {{erroProprietarioId}}, "anoFabricacao": 2024, "chassi": "{{erroNovoChassi}}", "renavam": "{{erroNovoRenavam}}", "placa": "{{erroPlaca}}"}
```

Asserções executadas:

- PASSOU: NEG-148: HTTP 409.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-149

**veiculo PATCH placa duplicado** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/149%20veiculo%20PATCH%20placa%20duplicado.bru).

`PATCH {{baseUrl}}/api/veiculos/{{erroVeiculo2Id}}` — esperado **409**, recebido **409**.

Corpo enviado:

```json
{"placa": "{{erroPlaca}}"}
```

Asserções executadas:

- PASSOU: NEG-149: HTTP 409.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-150

**proprietario POST JSON malformado** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/150%20proprietario%20POST%20JSON%20malformado.bru).

`POST {{baseUrl}}/api/proprietarios` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"campo":
```

Asserções executadas:

- PASSOU: NEG-150: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-151

**proprietario POST corpo null** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/151%20proprietario%20POST%20corpo%20null.bru).

`POST {{baseUrl}}/api/proprietarios` — esperado **400**, recebido **400**.

Corpo enviado:

```json
null
```

Asserções executadas:

- PASSOU: NEG-151: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-152

**proprietario POST array** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/152%20proprietario%20POST%20array.bru).

`POST {{baseUrl}}/api/proprietarios` — esperado **400**, recebido **400**.

Corpo enviado:

```json
[]
```

Asserções executadas:

- PASSOU: NEG-152: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-153

**proprietario POST sem corpo** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/153%20proprietario%20POST%20sem%20corpo.bru).

`POST {{baseUrl}}/api/proprietarios` — esperado **400**, recebido **400**.

Corpo: ausente.

Asserções executadas:

- PASSOU: NEG-153: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-154

**proprietario POST media type incorreto** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/154%20proprietario%20POST%20media%20type%20incorreto.bru).

`POST {{baseUrl}}/api/proprietarios` — esperado **415**, recebido **415**.

Corpo enviado:

```json
{}
```

Asserções executadas:

- PASSOU: NEG-154: HTTP 415.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-155

**proprietario PATCH JSON malformado** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/155%20proprietario%20PATCH%20JSON%20malformado.bru).

`PATCH {{baseUrl}}/api/proprietarios/{{erroProprietarioId}}` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"campo":
```

Asserções executadas:

- PASSOU: NEG-155: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-156

**proprietario PATCH corpo null** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/156%20proprietario%20PATCH%20corpo%20null.bru).

`PATCH {{baseUrl}}/api/proprietarios/{{erroProprietarioId}}` — esperado **400**, recebido **400**.

Corpo enviado:

```json
null
```

Asserções executadas:

- PASSOU: NEG-156: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-157

**proprietario PATCH array** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/157%20proprietario%20PATCH%20array.bru).

`PATCH {{baseUrl}}/api/proprietarios/{{erroProprietarioId}}` — esperado **400**, recebido **400**.

Corpo enviado:

```json
[]
```

Asserções executadas:

- PASSOU: NEG-157: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-158

**proprietario PATCH sem corpo** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/158%20proprietario%20PATCH%20sem%20corpo.bru).

`PATCH {{baseUrl}}/api/proprietarios/{{erroProprietarioId}}` — esperado **400**, recebido **400**.

Corpo: ausente.

Asserções executadas:

- PASSOU: NEG-158: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-159

**proprietario PATCH media type incorreto** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/159%20proprietario%20PATCH%20media%20type%20incorreto.bru).

`PATCH {{baseUrl}}/api/proprietarios/{{erroProprietarioId}}` — esperado **415**, recebido **415**.

Corpo enviado:

```json
{}
```

Asserções executadas:

- PASSOU: NEG-159: HTTP 415.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-160

**veiculo POST JSON malformado** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/160%20veiculo%20POST%20JSON%20malformado.bru).

`POST {{baseUrl}}/api/veiculos` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"campo":
```

Asserções executadas:

- PASSOU: NEG-160: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-161

**veiculo POST corpo null** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/161%20veiculo%20POST%20corpo%20null.bru).

`POST {{baseUrl}}/api/veiculos` — esperado **400**, recebido **400**.

Corpo enviado:

```json
null
```

Asserções executadas:

- PASSOU: NEG-161: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-162

**veiculo POST array** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/162%20veiculo%20POST%20array.bru).

`POST {{baseUrl}}/api/veiculos` — esperado **400**, recebido **400**.

Corpo enviado:

```json
[]
```

Asserções executadas:

- PASSOU: NEG-162: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-163

**veiculo POST sem corpo** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/163%20veiculo%20POST%20sem%20corpo.bru).

`POST {{baseUrl}}/api/veiculos` — esperado **400**, recebido **400**.

Corpo: ausente.

Asserções executadas:

- PASSOU: NEG-163: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-164

**veiculo POST media type incorreto** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/164%20veiculo%20POST%20media%20type%20incorreto.bru).

`POST {{baseUrl}}/api/veiculos` — esperado **415**, recebido **415**.

Corpo enviado:

```json
{}
```

Asserções executadas:

- PASSOU: NEG-164: HTTP 415.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-165

**veiculo PATCH JSON malformado** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/165%20veiculo%20PATCH%20JSON%20malformado.bru).

`PATCH {{baseUrl}}/api/veiculos/{{erroVeiculoId}}` — esperado **400**, recebido **400**.

Corpo enviado:

```json
{"campo":
```

Asserções executadas:

- PASSOU: NEG-165: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-166

**veiculo PATCH corpo null** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/166%20veiculo%20PATCH%20corpo%20null.bru).

`PATCH {{baseUrl}}/api/veiculos/{{erroVeiculoId}}` — esperado **400**, recebido **400**.

Corpo enviado:

```json
null
```

Asserções executadas:

- PASSOU: NEG-166: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-167

**veiculo PATCH array** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/167%20veiculo%20PATCH%20array.bru).

`PATCH {{baseUrl}}/api/veiculos/{{erroVeiculoId}}` — esperado **400**, recebido **400**.

Corpo enviado:

```json
[]
```

Asserções executadas:

- PASSOU: NEG-167: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-168

**veiculo PATCH sem corpo** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/168%20veiculo%20PATCH%20sem%20corpo.bru).

`PATCH {{baseUrl}}/api/veiculos/{{erroVeiculoId}}` — esperado **400**, recebido **400**.

Corpo: ausente.

Asserções executadas:

- PASSOU: NEG-168: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-169

**veiculo PATCH media type incorreto** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/169%20veiculo%20PATCH%20media%20type%20incorreto.bru).

`PATCH {{baseUrl}}/api/veiculos/{{erroVeiculoId}}` — esperado **415**, recebido **415**.

Corpo enviado:

```json
{}
```

Asserções executadas:

- PASSOU: NEG-169: HTTP 415.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-170

**proprietarios PUT nao permitido** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/170%20proprietarios%20PUT%20nao%20permitido.bru).

`PUT {{baseUrl}}/api/proprietarios` — esperado **405**, recebido **405**.

Corpo enviado:

```json
{}
```

Asserções executadas:

- PASSOU: NEG-170: HTTP 405.
- PASSOU: Contrato de erro e ausencia de detalhes internos.
- PASSOU: Efeito esperado.

### NEG-171

**veiculos PUT nao permitido** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/171%20veiculos%20PUT%20nao%20permitido.bru).

`PUT {{baseUrl}}/api/veiculos` — esperado **405**, recebido **405**.

Corpo enviado:

```json
{}
```

Asserções executadas:

- PASSOU: NEG-171: HTTP 405.
- PASSOU: Contrato de erro e ausencia de detalhes internos.
- PASSOU: Efeito esperado.

### NEG-172

**historico PUT nao permitido** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/172%20historico%20PUT%20nao%20permitido.bru).

`PUT {{baseUrl}}/api/historico` — esperado **405**, recebido **405**.

Corpo enviado:

```json
{}
```

Asserções executadas:

- PASSOU: NEG-172: HTTP 405.
- PASSOU: Contrato de erro e ausencia de detalhes internos.
- PASSOU: Efeito esperado.

### NEG-173

**log-proprietarios PUT nao permitido** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/173%20log-proprietarios%20PUT%20nao%20permitido.bru).

`PUT {{baseUrl}}/api/log-proprietarios` — esperado **405**, recebido **405**.

Corpo enviado:

```json
{}
```

Asserções executadas:

- PASSOU: NEG-173: HTTP 405.
- PASSOU: Contrato de erro e ausencia de detalhes internos.
- PASSOU: Efeito esperado.

### NEG-174

**historico POST proibido** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/174%20historico%20POST%20proibido.bru).

`POST {{baseUrl}}/api/historico` — esperado **405**, recebido **405**.

Corpo enviado:

```json
{}
```

Asserções executadas:

- PASSOU: NEG-174: HTTP 405.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-175

**historico PATCH proibido** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/175%20historico%20PATCH%20proibido.bru).

`PATCH {{baseUrl}}/api/historico/1` — esperado **405**, recebido **405**.

Corpo enviado:

```json
{}
```

Asserções executadas:

- PASSOU: NEG-175: HTTP 405.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-176

**historico DELETE proibido** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/176%20historico%20DELETE%20proibido.bru).

`DELETE {{baseUrl}}/api/historico/1` — esperado **405**, recebido **405**.

Corpo: ausente.

Asserções executadas:

- PASSOU: NEG-176: HTTP 405.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-177

**log-proprietarios POST proibido** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/177%20log-proprietarios%20POST%20proibido.bru).

`POST {{baseUrl}}/api/log-proprietarios` — esperado **405**, recebido **405**.

Corpo enviado:

```json
{}
```

Asserções executadas:

- PASSOU: NEG-177: HTTP 405.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-178

**log-proprietarios PATCH proibido** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/178%20log-proprietarios%20PATCH%20proibido.bru).

`PATCH {{baseUrl}}/api/log-proprietarios/1` — esperado **405**, recebido **405**.

Corpo enviado:

```json
{}
```

Asserções executadas:

- PASSOU: NEG-178: HTTP 405.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-179

**log-proprietarios DELETE proibido** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/179%20log-proprietarios%20DELETE%20proibido.bru).

`DELETE {{baseUrl}}/api/log-proprietarios/1` — esperado **405**, recebido **405**.

Corpo: ausente.

Asserções executadas:

- PASSOU: NEG-179: HTTP 405.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-180

**Rota inexistente** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/180%20Rota%20inexistente.bru).

`GET {{baseUrl}}/api/rota-que-nao-existe` — esperado **404**, recebido **404**.

Corpo: ausente.

Asserções executadas:

- PASSOU: NEG-180: HTTP 404.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-181

**Ordenacao por atributo inexistente** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/181%20Ordenacao%20por%20atributo%20inexistente.bru).

`GET {{baseUrl}}/api/veiculos?sort=atributoInexistente,asc` — esperado **400**, recebido **500**.

Corpo: ausente.

Expectativa de robustez: entrada de ordenação inválida deve gerar 400; 500 é falha da API.

Asserções executadas:

- FALHOU: NEG-181: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

Resposta recebida:

```json
{
  "dataHora": "23/10/2026 11:32:35",
  "status": 500,
  "excecao": "PropertyReferenceException",
  "mensagem": "Ocorreu um erro interno. Tente novamente mais tarde.",
  "erros": []
}
```

### NEG-182

**Paginacao com offset maior que limite JPA** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/182%20Paginacao%20com%20offset%20maior%20que%20limite%20JPA.bru).

`GET {{baseUrl}}/api/veiculos?page=2147483647` — esperado **400**, recebido **500**.

Corpo: ausente.

Expectativa de robustez: rejeitar página cujo offset não cabe no limite JPA; 500 é falha da API.

Asserções executadas:

- FALHOU: NEG-182: HTTP 400.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

Resposta recebida:

```json
{
  "dataHora": "23/10/2026 11:32:35",
  "status": 500,
  "excecao": "InvalidDataAccessApiUsageException",
  "mensagem": "Ocorreu um erro interno. Tente novamente mais tarde.",
  "erros": []
}
```

### NEG-183

**Paginacao normaliza page -1** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/183%20Paginacao%20normaliza%20page%20-1.bru).

`GET {{baseUrl}}/api/veiculos?page=-1` — esperado **200**, recebido **200**.

Corpo: ausente.

Comportamento atual do resolver Spring: valor inválido usa padrão e tamanho fica limitado a 15.

Asserções executadas:

- PASSOU: NEG-183: HTTP 200.
- PASSOU: Efeito esperado.

### NEG-184

**Paginacao normaliza page abc** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/184%20Paginacao%20normaliza%20page%20abc.bru).

`GET {{baseUrl}}/api/veiculos?page=abc` — esperado **200**, recebido **200**.

Corpo: ausente.

Comportamento atual do resolver Spring: valor inválido usa padrão e tamanho fica limitado a 15.

Asserções executadas:

- PASSOU: NEG-184: HTTP 200.
- PASSOU: Efeito esperado.

### NEG-185

**Paginacao normaliza size 0** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/185%20Paginacao%20normaliza%20size%200.bru).

`GET {{baseUrl}}/api/veiculos?size=0` — esperado **200**, recebido **200**.

Corpo: ausente.

Comportamento atual do resolver Spring: valor inválido usa padrão e tamanho fica limitado a 15.

Asserções executadas:

- PASSOU: NEG-185: HTTP 200.
- PASSOU: Efeito esperado.

### NEG-186

**Paginacao normaliza size -1** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/186%20Paginacao%20normaliza%20size%20-1.bru).

`GET {{baseUrl}}/api/veiculos?size=-1` — esperado **200**, recebido **200**.

Corpo: ausente.

Comportamento atual do resolver Spring: valor inválido usa padrão e tamanho fica limitado a 15.

Asserções executadas:

- PASSOU: NEG-186: HTTP 200.
- PASSOU: Efeito esperado.

### NEG-187

**Paginacao normaliza size abc** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/187%20Paginacao%20normaliza%20size%20abc.bru).

`GET {{baseUrl}}/api/veiculos?size=abc` — esperado **200**, recebido **200**.

Corpo: ausente.

Comportamento atual do resolver Spring: valor inválido usa padrão e tamanho fica limitado a 15.

Asserções executadas:

- PASSOU: NEG-187: HTTP 200.
- PASSOU: Efeito esperado.

### NEG-188

**Paginacao normaliza size 100** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/188%20Paginacao%20normaliza%20size%20100.bru).

`GET {{baseUrl}}/api/veiculos?size=100` — esperado **200**, recebido **200**.

Corpo: ausente.

Comportamento atual do resolver Spring: valor inválido usa padrão e tamanho fica limitado a 15.

Asserções executadas:

- PASSOU: NEG-188: HTTP 200.
- PASSOU: Efeito esperado.

### NEG-189

**Pagina fora dos resultados** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/189%20Pagina%20fora%20dos%20resultados.bru).

`GET {{baseUrl}}/api/veiculos?page=100000` — esperado **200**, recebido **200**.

Corpo: ausente.

Asserções executadas:

- PASSOU: NEG-189: HTTP 200.
- PASSOU: Efeito esperado.

### NEG-190

**proprietario PATCH vazio preserva dados** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/190%20proprietario%20PATCH%20vazio%20preserva%20dados.bru).

`PATCH {{baseUrl}}/api/proprietarios/{{erroProprietarioId}}` — esperado **200**, recebido **200**.

Corpo enviado:

```json
{}
```

Asserções executadas:

- PASSOU: NEG-190: HTTP 200.
- PASSOU: Efeito esperado.

### NEG-191

**proprietario PATCH campos nulos preserva dados** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/191%20proprietario%20PATCH%20campos%20nulos%20preserva%20dados.bru).

`PATCH {{baseUrl}}/api/proprietarios/{{erroProprietarioId}}` — esperado **200**, recebido **200**.

Corpo enviado:

```json
{"nome": null, "cpfCnpj": null}
```

Asserções executadas:

- PASSOU: NEG-191: HTTP 200.
- PASSOU: Efeito esperado.

### NEG-192

**proprietario falhas nao alteraram registro** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/192%20proprietario%20falhas%20nao%20alteraram%20registro.bru).

`GET {{baseUrl}}/api/proprietarios/{{erroProprietarioId}}` — esperado **200**, recebido **200**.

Corpo: ausente.

Asserções executadas:

- PASSOU: NEG-192: HTTP 200.
- PASSOU: Efeito esperado.

### NEG-193

**veiculo PATCH vazio preserva dados** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/193%20veiculo%20PATCH%20vazio%20preserva%20dados.bru).

`PATCH {{baseUrl}}/api/veiculos/{{erroVeiculoId}}` — esperado **200**, recebido **200**.

Corpo enviado:

```json
{}
```

Asserções executadas:

- PASSOU: NEG-193: HTTP 200.
- PASSOU: Efeito esperado.

### NEG-194

**veiculo PATCH campos nulos preserva dados** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/194%20veiculo%20PATCH%20campos%20nulos%20preserva%20dados.bru).

`PATCH {{baseUrl}}/api/veiculos/{{erroVeiculoId}}` — esperado **200**, recebido **200**.

Corpo enviado:

```json
{"tipo": null, "placa": null, "anoFabricacao": null}
```

Asserções executadas:

- PASSOU: NEG-194: HTTP 200.
- PASSOU: Efeito esperado.

### NEG-195

**veiculo falhas nao alteraram registro** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/195%20veiculo%20falhas%20nao%20alteraram%20registro.bru).

`GET {{baseUrl}}/api/veiculos/{{erroVeiculoId}}` — esperado **200**, recebido **200**.

Corpo: ausente.

Asserções executadas:

- PASSOU: NEG-195: HTTP 200.
- PASSOU: Efeito esperado.

### NEG-196

**segundo veiculo conflitos nao alteraram registro** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/196%20segundo%20veiculo%20conflitos%20nao%20alteraram%20registro.bru).

`GET {{baseUrl}}/api/veiculos/{{erroVeiculo2Id}}` — esperado **200**, recebido **200**.

Corpo: ausente.

Asserções executadas:

- PASSOU: NEG-196: HTTP 200.
- PASSOU: Efeito esperado.

### NEG-197

**Excluir veiculo de teste** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/197%20Excluir%20veiculo%20de%20teste.bru).

`DELETE {{baseUrl}}/api/veiculos/{{erroVeiculoId}}` — esperado **204**, recebido **204**.

Corpo: ausente.

Asserções executadas:

- PASSOU: NEG-197: HTTP 204.

### NEG-198

**veiculo excluido GET** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/198%20veiculo%20excluido%20GET.bru).

`GET {{baseUrl}}/api/veiculos/{{erroVeiculoId}}` — esperado **404**, recebido **404**.

Corpo: ausente.

Asserções executadas:

- PASSOU: NEG-198: HTTP 404.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-199

**veiculo excluido PATCH** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/199%20veiculo%20excluido%20PATCH.bru).

`PATCH {{baseUrl}}/api/veiculos/{{erroVeiculoId}}` — esperado **404**, recebido **404**.

Corpo enviado:

```json
{}
```

Asserções executadas:

- PASSOU: NEG-199: HTTP 404.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-200

**veiculo excluido DELETE** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/200%20veiculo%20excluido%20DELETE.bru).

`DELETE {{baseUrl}}/api/veiculos/{{erroVeiculoId}}` — esperado **404**, recebido **404**.

Corpo: ausente.

Asserções executadas:

- PASSOU: NEG-200: HTTP 404.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-201

**Excluir proprietario e veiculo em cascata** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/201%20Excluir%20proprietario%20e%20veiculo%20em%20cascata.bru).

`DELETE {{baseUrl}}/api/proprietarios/{{erroProprietarioId}}` — esperado **204**, recebido **204**.

Corpo: ausente.

Asserções executadas:

- PASSOU: NEG-201: HTTP 204.

### NEG-202

**proprietario excluido GET** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/202%20proprietario%20excluido%20GET.bru).

`GET {{baseUrl}}/api/proprietarios/{{erroProprietarioId}}` — esperado **404**, recebido **404**.

Corpo: ausente.

Asserções executadas:

- PASSOU: NEG-202: HTTP 404.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-203

**proprietario excluido PATCH** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/203%20proprietario%20excluido%20PATCH.bru).

`PATCH {{baseUrl}}/api/proprietarios/{{erroProprietarioId}}` — esperado **404**, recebido **404**.

Corpo enviado:

```json
{}
```

Asserções executadas:

- PASSOU: NEG-203: HTTP 404.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-204

**proprietario excluido DELETE** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/204%20proprietario%20excluido%20DELETE.bru).

`DELETE {{baseUrl}}/api/proprietarios/{{erroProprietarioId}}` — esperado **404**, recebido **404**.

Corpo: ausente.

Asserções executadas:

- PASSOU: NEG-204: HTTP 404.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-205

**veiculo cascata excluido GET** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/205%20veiculo%20cascata%20excluido%20GET.bru).

`GET {{baseUrl}}/api/veiculos/{{erroVeiculo2Id}}` — esperado **404**, recebido **404**.

Corpo: ausente.

Asserções executadas:

- PASSOU: NEG-205: HTTP 404.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-206

**veiculo cascata excluido PATCH** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/206%20veiculo%20cascata%20excluido%20PATCH.bru).

`PATCH {{baseUrl}}/api/veiculos/{{erroVeiculo2Id}}` — esperado **404**, recebido **404**.

Corpo enviado:

```json
{}
```

Asserções executadas:

- PASSOU: NEG-206: HTTP 404.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-207

**veiculo cascata excluido DELETE** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/207%20veiculo%20cascata%20excluido%20DELETE.bru).

`DELETE {{baseUrl}}/api/veiculos/{{erroVeiculo2Id}}` — esperado **404**, recebido **404**.

Corpo: ausente.

Asserções executadas:

- PASSOU: NEG-207: HTTP 404.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-208

**Cadastrar veiculo com dono inativo** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/208%20Cadastrar%20veiculo%20com%20dono%20inativo.bru).

`POST {{baseUrl}}/api/veiculos` — esperado **404**, recebido **404**.

Corpo enviado:

```json
{"tipo": "CARRO", "idProprietario": {{erroProprietarioId}}, "anoFabricacao": 2024, "chassi": "{{erroChassi}}", "renavam": "{{erroRenavam}}", "placa": "{{erroPlaca}}"}
```

Asserções executadas:

- PASSOU: NEG-208: HTTP 404.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-209

**CPF excluido continua reservado** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/209%20CPF%20excluido%20continua%20reservado.bru).

`POST {{baseUrl}}/api/proprietarios` — esperado **409**, recebido **409**.

Corpo enviado:

```json
{"cpfCnpj": "{{erroCpf}}", "nome": "Teste Cenarios Erro", "cep": "60000000", "estado": "CE"}
```

Asserções executadas:

- PASSOU: NEG-209: HTTP 409.
- PASSOU: Contrato de erro e ausencia de detalhes internos.

### NEG-210

**Listagem oculta proprietario inativo** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/210%20Listagem%20oculta%20proprietario%20inativo.bru).

`GET {{baseUrl}}/api/proprietarios` — esperado **200**, recebido **200**.

Corpo: ausente.

Asserções executadas:

- PASSOU: NEG-210: HTTP 200.
- PASSOU: Efeito esperado.

### NEG-211

**Listagem oculta veiculos inativos** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/211%20Listagem%20oculta%20veiculos%20inativos.bru).

`GET {{baseUrl}}/api/veiculos` — esperado **200**, recebido **200**.

Corpo: ausente.

Asserções executadas:

- PASSOU: NEG-211: HTTP 200.
- PASSOU: Efeito esperado.

### NEG-212

**Historico preservado apos exclusao** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/212%20Historico%20preservado%20apos%20exclusao.bru).

`GET {{baseUrl}}/api/historico` — esperado **200**, recebido **200**.

Corpo: ausente.

Asserções executadas:

- PASSOU: NEG-212: HTTP 200.
- PASSOU: Efeito esperado.

### NEG-213

**Log preservado apos exclusao** — [Abrir teste](../../bruno/06%20Cenarios%20de%20erro/213%20Log%20preservado%20apos%20exclusao.bru).

`GET {{baseUrl}}/api/log-proprietarios` — esperado **200**, recebido **200**.

Corpo: ausente.

Asserções executadas:

- PASSOU: NEG-213: HTTP 200.
- PASSOU: Efeito esperado.

## Testes Java

| Classe | Testes | Falhas | Erros | Ignorados |
| --- | --- | --- | --- | --- |
| ApiExceptionIntegrationTest | 11 | 0 | 0 | 0 |
| ApplicationTests | 1 | 0 | 0 | 0 |
| DocumentoTest | 11 | 0 | 0 | 0 |
| GlobalExceptionTest | 16 | 0 | 0 | 0 |
| OutrosDTOsTest | 3 | 0 | 0 | 0 |
| ProprietarioDTOTest | 3 | 0 | 0 | 0 |
| ProprietarioValidacaoTest | 27 | 0 | 0 | 0 |
| VeiculoServiceTest | 6 | 0 | 0 | 0 |

## Evidências

- [Relatório HTML](resultado-bruno.html).
- [Respostas e asserções em JSON](resultado-bruno.json).
- [JUnit XML](resultado-bruno.xml).
- [Matriz estruturada dos cenários](cenarios.json).

A suíte cobre validações e falhas funcionais previsíveis pelos endpoints atuais. Concorrência, carga, falhas de rede/banco e autenticação não foram simuladas. IDs de auditoria e dados fictícios constam nos relatórios. Exclusão é lógica: histórico e logs dos testes permanecem no banco.
