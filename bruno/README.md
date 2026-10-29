# Collection Bruno

1. Inicie a API com o PostgreSQL configurado em `application.properties`.
2. No Bruno, use **Open Collection** e selecione esta pasta (`bruno`).
3. Selecione o ambiente **Local**. Altere `baseUrl` se a API estiver em outro endereço.
4. Execute a collection pelo Runner, em ordem sequencial, das pastas `01` a `06`.

As 21 requisições das pastas `01` a `05` cobrem cadastro, listagem, busca, atualização parcial,
paginação de veículos, consulta de auditoria, respostas de validação e conflito,
exclusão lógica e confirmação de `404` após excluir.

O primeiro cadastro gera um CPF válido e dados de veículo novos. Os scripts
capturam os IDs das respostas em variáveis de execução, sem depender de IDs
preexistentes. Execute os cadastros antes das requisições que usam esses IDs.
Histórico e logs dependem das triggers do banco descritas no README do projeto.

O fluxo grava dados no banco apontado pela API. A pasta `05 Exclusao` inativa
os registros criados; histórico e logs permanecem, conforme as regras da API.
Se interromper a execução, execute essa pasta na mesma sessão para finalizar
a exclusão dos registros de teste.

Os scripts usam a [API de variáveis e respostas do Bruno](https://github.com/usebruno/bruno-docs/blob/main/testing/script/javascript-reference.mdx).

## Cenários adicionais de erro

A pasta `06 Cenarios de erro` contém 213 cenários com preparação própria,
validações, conflitos, erros de protocolo, exclusão lógica, paginação e
preservação de dados. Ela também pode ser executada sozinha, em ordem.
Selecione `Testes` para a instância de teste na porta 8081.
O ambiente `Local` continua apontando para a porta 8080.

O [documento de cenários e resultados](../docs/testes/CENARIOS-E-RESULTADOS.md)
descreve cada requisição, corpo, resultado esperado, asserções executadas e
falhas encontradas. Inclui comandos para iniciar a API com o endereço explícito
do banco `dbtreino_test` e executar a coleção com o Bruno CLI.

Asserções de identificação de campos inválidos no PATCH e rejeição de ordenação
ou página extrema revelam falhas atuais da API. A execução completa retorna
código de saída diferente de zero enquanto essas falhas persistirem.
Execute sequencialmente, sem `--bail`, para que a limpeza final seja executada.
Não execute a suíte Java simultaneamente com o Bruno no mesmo banco.
