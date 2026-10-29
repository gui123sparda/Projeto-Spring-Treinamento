# Plano dos testes de endpoints

Objetivo: revisar os 14 endpoints, documentar cenários reproduzíveis e executar
os testes na coleção Bruno já registrada no workspace do usuário.

Decisões: preservar as requisições existentes; acrescentar uma pasta independente
com preparação, testes negativos e limpeza; executar HTTP real na porta 8081
com PostgreSQL `dbtreino_test`; manter falhas encontradas visíveis, sem alterar
regras da API para fazer testes passarem. Não há repositório Git neste diretório.

- [x] Mapear controllers, DTOs, serviços, restrições e triggers.
- [x] Localizar a coleção no workspace Bruno e executar a suíte Java inicial.
- [x] Criar matriz de cenários e requisições com asserções de status e corpo.
- [x] Executar toda a coleção sequencialmente e preservar evidências.
- [x] Documentar cada cenário, resultado, reprodução e falhas encontradas.

Verificação: `./mvnw test` com Java 25; Bruno CLI 4.3.0 com relatórios JSON,
JUnit e HTML; conferir registros de teste inativos após a execução.

Resultado: 213 cenários novos, 234 requisições executadas, 212 aprovadas e 22
falhas reproduzíveis (20 de detalhamento de validação PATCH e dois erros 500).
A suíte Java passou com 78 testes. Conferência SQL final: zero proprietários
ativos e zero veículos ativos no banco de testes. A API não foi modificada.

Ocorrência: a execução exploratória usou `dbtreino` porque o perfil test não
incluiu os recursos de teste no `spring-boot:run`. Os registros gerados foram
inativados e a execução final foi repetida com a URL explícita `dbtreino_test`.
Detalhes e impactos estão no documento de cenários e resultados.
