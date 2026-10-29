# API de proprietários e veículos

Requer Java 25, Maven e PostgreSQL. A aplicação usa o banco `dbtreino`, esquema
`treino`, e valida o esquema existente sem recriar suas tabelas.

## Swagger e logs

Com a aplicação iniciada, abra http://localhost:8080/swagger-ui/index.html
para consultar e testar as rotas REST. O documento OpenAPI está em
http://localhost:8080/v3/api-docs. As telas HTML ficam fora dessa documentação.
A dependência springdoc 3.x segue a [compatibilidade oficial com Spring Boot 4](https://springdoc.org/faq.html).

O banner de inicialização usa a arte CYBERPUNK colorida e mostra
**Treinamento CRUD Spring Boot**, a versão e o responsável (padrão `DETRAN-CE`).
Use `APP_VERSION` e `APP_OWNER` para personalizar esses dois campos. Esse nome
também identifica cada linha dos logs, inclusive stack traces, no console e em
`logs/treinamento-crud-spring.log`. Use `APP_NAME` para alterar o nome e
`LOG_FILE` para mudar o arquivo. Os arquivos são rotacionados a cada 10 MB,
com retenção de até 14 dias e limite total de arquivos históricos de 200 MB.

Cada requisição HTTP registra início, método, caminho, status e duração em
milissegundos. O `requestId` acompanha os logs na thread da requisição e é
devolvido no cabeçalho `X-Request-Id`. Respostas 4xx usam WARN e 5xx usam ERROR;
as demais usam INFO. Corpos, parâmetros de consulta e cabeçalhos de autenticação
não são registrados pelo filtro. Erros inesperados incluem stack trace.

O SQL usa o logger do Hibernate, com o mesmo formato dos demais logs, sem
registrar os valores vinculados aos parâmetros. `APP_LOG_LEVEL` controla os
logs do projeto (padrão `DEBUG`) e `SQL_LOG_LEVEL` controla o SQL (padrão `DEBUG`;
use `OFF` para desativar). Os logs técnicos complementam o histórico de
auditoria já mantido pelas triggers do banco.

## Testes

Prepare uma vez um banco separado, vazio, com as mesmas tabelas, enums e triggers:

```sh
createdb -h localhost -U postgres dbtreino_test
psql -h localhost -U postgres -d dbtreino_test -v ON_ERROR_STOP=1 -f src/test/resources/schema-test.sql
```

O script contém somente a estrutura, sem dados. Execute-o apenas no banco de
testes vazio. As credenciais dos testes podem ser configuradas por
`TEST_DB_USERNAME` e `TEST_DB_PASSWORD` (padrão: `postgres`).

Com Java 25 selecionado, execute:

```sh
./mvnw test
```

Os testes de integração usam `dbtreino_test`, validam o esquema e revertem as
transações ao final de cada teste. As sequências de IDs podem avançar.

Os formulários de proprietário (PF/PJ) e veículo oferecem o botão **Gerar dados
fictícios**, seguindo o projeto servlet. Ele preenche os campos para revisão antes
de salvar, com CPF/CNPJ e Renavam com dígitos verificadores válidos. Na edição,
preserva o CPF/CNPJ; no veículo, mantém o proprietário selecionado e só fica
disponível após essa seleção.

Para testar a geração e o preenchimento dos dados, com Node.js instalado:

```sh
node --test src/test/js/dados-ficticios.test.cjs
```

## Comportamento da API

A collection para testar a API no Bruno está em [`bruno/`](bruno/README.md).
Abra essa pasta no Bruno, selecione o ambiente `Local` e execute as pastas em
ordem pelo Runner. O fluxo cria registros de teste e os exclui logicamente ao final.

Os [cenários de teste e resultados](docs/testes/CENARIOS-E-RESULTADOS.md)
documentam a revisão de todos os endpoints e os 213 cenários adicionais da
pasta `06 Cenarios de erro` no Bruno, com evidências de execução e falhas encontradas.

- Proprietários: `GET`, `POST` em `/api/proprietarios`; `GET`, `PATCH`, `DELETE` em
  `/api/proprietarios/{id}`.
- Veículos: `GET`, `POST` em `/api/veiculos`; `GET`, `PATCH`, `DELETE` em
  `/api/veiculos/{id}`. A listagem aceita `page`, `size` e `sort`, com páginas
  iniciando em zero e limite de 15 registros por página.
- Histórico e logs: somente `GET` na coleção e em `/{id}`, nas rotas
  `/api/historico` e `/api/log-proprietarios`.

O `PATCH` preserva campos ausentes ou nulos. CPF/CNPJ não pode ser alterado após
o cadastro. Os tipos de veículo aceitos são `CARRO` e `MOTO`.

O `DELETE` preenche `excluido_em`. As consultas omitem registros inativos, que
passam a responder `404` nas operações por ID. A trigger do banco também inativa
os veículos do proprietário excluído. Histórico e logs são gerados pelas triggers.

## Notificações do aplicativo nas telas web

As telas web usam notificações nativas do navegador quando o aplicativo
cadastra, edita ou exclui um proprietário ou veículo. Clique em **Ativar
notificações**, no cabeçalho, e permita o envio no navegador. A ativação exige
HTTPS ou acesso por localhost e um navegador compatível. Mantenha uma tela web
aberta: o aviso aparece em até cerca de 2 segundos após a confirmação da
operação no banco, inclusive quando a aba estiver em segundo plano (sujeito
à frequência de execução permitida pelo navegador).

As operações feitas pelas rotas `/api/**` são consideradas atividade do
aplicativo, incluindo outros clientes que usem essas rotas. Os formulários
web exibem a confirmação local também como notificação. Não é necessário
configurar `WEBHOOK_URL` ou alterar o Flutter. A permissão só é solicitada após
clicar no botão; se estiver bloqueada, libere-a nas configurações do site.

O navegador consulta `GET /notificacoes` para iniciar o acompanhamento e
`GET /notificacoes?apos=<cursor>` para receber os eventos seguintes. O servidor
mantém os últimos 200 eventos em memória por instância; ao reconectar, o
navegador recupera os eventos ainda disponíveis. Quando a notificação nativa
estiver indisponível ou bloqueada, o aviso aparece no canto superior direito,
sem ocupar espaço no formulário. São exibidos até três avisos flutuantes, com
botão de fechar e remoção após 10 segundos. A aba guarda o cursor para acompanhar as
novas alterações sem repetir avisos já recebidos. O buffer do servidor se
perde no reinício e não constitui um histórico permanente.
Uma instalação com várias instâncias precisará de armazenamento compartilhado.

Os avisos mostram o nome do proprietário ou a placa do veículo, capturados no
momento da operação, inclusive para exclusões. A aparência da notificação
nativa segue o navegador e o sistema operacional. Nos avisos flutuantes,
cadastro aparece em verde, edição em azul e exclusão em vermelho, com cores
adaptadas ao tema escuro.
CPF/CNPJ, chassi e Renavam não são incluídos nas notificações. O webhook
externo continua recebendo apenas os metadados, sem o nome ou a placa.
Falhas e transações desfeitas não geram notificações. A exclusão de um
proprietário gera um aviso, incluindo a inativação em cascata de seus veículos.
O teste `NotificacoesTest` verifica as seis operações da API, commit/rollback,
distinção dos formulários web, retomada por cursor e limite do histórico.
Os testes de apresentação, permissão e alternativa flutuante rodam com
`node --test src/test/js/*.test.cjs`.

## Webhook externo opcional

O backend envia um `POST` JSON para `WEBHOOK_URL` após o commit de cada cadastro,
alteração ou exclusão lógica de proprietário ou veículo. Isso atende tanto o
app Flutter quanto as telas web, sem uma chamada adicional no aplicativo.

Configure o endereço do servidor que receberá as notificações e reinicie o Spring:

```sh
export WEBHOOK_URL=https://seu-servidor.example/webhooks/cadastros
export WEBHOOK_TOKEN=seu-token
./mvnw spring-boot:run
```

`WEBHOOK_TOKEN` é opcional. Quando preenchido, o POST inclui
`Authorization: Bearer <token>`. Sem `WEBHOOK_URL`, o envio fica desativado.
O destino precisa aceitar POST; não foi criada uma rota receptora neste backend.

Exemplo de corpo enviado:

```json
{
  "eventoId": "70b23756-9c59-4059-a5f1-f672bd079860",
  "entidade": "veiculo",
  "operacao": "CADASTRO",
  "registroId": 73,
  "ocorridoEm": "2026-10-27T10:00:00-03:00"
}
```

`entidade` pode ser `proprietario` ou `veiculo`; `operacao` pode ser `CADASTRO`,
`ALTERACAO` ou `EXCLUSAO_LOGICA`. O ID é o registro afetado; `eventoId` identifica
a notificação. O payload não inclui dados pessoais. A exclusão de proprietário
gera um evento do proprietário, abrangendo a inativação em cascata dos veículos;
as alterações feitas diretamente por triggers ou SQL não geram eventos separados.

O envio é assíncrono, com timeout de 3 segundos. O receptor deve responder com
HTTP 2xx. Falhas de conexão, timeout e respostas fora de 2xx são registradas no
log com o ID do evento e não desfazem o cadastro nem alteram sua resposta HTTP.
Não há fila persistente ou retentativas: a entrega é uma tentativa e pode ser
perdida se o processo encerrar ou o destino estiver indisponível.

Os testes `CadastroWebhookTest` usam um receptor HTTP local para verificar as
seis combinações de entidade/operação, os limites de commit/rollback, falhas de
persistência, envio desativado e resposta lenta ou HTTP 500 do destino.

## Flutter Web e CORS

As rotas `/api/**` permitem acesso pelo navegador a partir de
`http://localhost` e `http://127.0.0.1`, em qualquer porta local. A configuração
`CorsConfig` inclui os métodos `GET`, `POST`, `PATCH`, `DELETE` e `OPTIONS`,
com os cabeçalhos `Content-Type` e `Accept`. As chamadas nativas Android e
as consultas sem `Origin` continuam funcionando normalmente.

Depois de atualizar a configuração, reinicie a API. Execute o frontend com:

```sh
flutter run -d chrome --web-port=5173 --dart-define=API_BASE_URL=http://localhost:8080
```

Para outra origem, configure `CORS_ALLOWED_ORIGIN_PATTERNS` antes de iniciar o
Spring Boot, com as origens permitidas separadas por vírgula. Esse valor
substitui a lista local padrão; inclua as origens locais se ainda forem necessárias.
A configuração se aplica às rotas REST, sem alterar as rotas da interface web.
Os testes `CorsApiTest` verificam leitura, preflight de escrita, rejeição de
origem externa e acesso sem `Origin`.

## Interface web

- `/veiculos`: lista paginada, cadastro, detalhes, edição e exclusão lógica.
- `/log-proprietarios`: consulta e detalhes dos logs de proprietários.
- `/historico`: consulta da auditoria, incluindo os valores antes e depois.

As telas utilizam os mesmos serviços da API. Logs e histórico são somente
leitura e continuam sendo gerados automaticamente pelas triggers do banco.

### Fluxo de veículos e placas

O cadastro começa pela seleção do proprietário e libera os dados do veículo em
seguida. O botão **Cadastrar proprietário** retorna ao cadastro do veículo com o
novo proprietário selecionado. Salvar ou editar retorna à lista de veículos.

- `/veiculos/consultar-placa`: consulta por placa, com dados do veículo e proprietário.
- `/proprietarios/{id}/veiculos`: veículos vinculados e cadastro com proprietário selecionado.
- `/veiculos/{id}/placa.png`: imagem ilustrativa da placa, usando o renderizador e
  os recursos de fontes do projeto `TreinamentoCRUDServlet`.

A visualização usa os formatos antigo e Mercosul, para carro e moto, com hover 3D
no padrão do projeto base. A preferência por movimento reduzido desativa a animação.

### Fluxo de telas e relatórios do projeto Servlet

A navegação segue o `TreinamentoCRUDServlet`: consultas separadas de pessoas
físicas e empresas, detalhes do proprietário com seus veículos, consulta de
placa com proprietário e imagem da placa e históricos separados por entidade.
Ao editar um proprietário, o retorno é à lista PF/PJ correspondente. Ao excluir
um veículo pela lista do proprietário, o retorno permanece nessa lista.

Os botões **Exportar PDF** usam os modelos Jasper do projeto base, com suas
fontes e imagens institucionais. As listagens exportam todos os resultados dos
filtros, sem o limite de registros da página exibida.

| Rota GET | Relatório |
| --- | --- |
| `/relatorio-proprietarios` | Lista filtrada por `campo`, `valor` e `tipoPessoa` |
| `/relatorio-proprietario?id=…` | Cadastro individual de pessoa física ou empresa |
| `/relatorio-veiculos` | Lista filtrada por `campo` e `valor` |
| `/relatorio-veiculo?id=…` | Veículo, placa e proprietário |
| `/relatorio-agrupado?idProprietario=…` | Proprietário e todos os seus veículos ativos |
| `/relatorio-historico` | Histórico de proprietários |
| `/relatorio-historico-veiculos` | Histórico de veículos |

Os históricos `/historico` e `/historico-veiculos` e seus PDFs aceitam os mesmos
filtros: `busca`, `registroId`, `operacao`, `dataDe` e `dataAte`. As datas usam o
fuso `America/Fortaleza`, incluindo todo o último dia informado. A busca por
conteúdo usa a função existente `treino.normalizar_busca` e a extensão
PostgreSQL `unaccent`, incluída no script de preparação do banco de testes.

As operações permanecem nos serviços existentes `ProprietarioService`,
`VeiculoService` e `HistoricoService`. A classe utilitária `Relatorios` adapta os
campos e renderiza os modelos, sem criar um novo service.

Os fontes estão em `src/main/resources/jxml` e os modelos compilados em
`src/main/resources/jasper`. O modelo de histórico preserva o layout original,
com a expressão que dependia de uma classe Servlet adaptada aos campos já
tratados pelo Spring. Após editar um `.jrxml`, recompile com Java 25:

```sh
./tools/compilar-relatorios.sh
./mvnw test
```
