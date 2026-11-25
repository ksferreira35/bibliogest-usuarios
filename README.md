# BiblioGest — Usuários

API de administração de contas em Java 21 e Spring Boot 4.1.2. Integra o realm `bibliogest` do Keycloak; o PostgreSQL guarda apenas a auditoria, sem duplicar senhas ou o cadastro de usuários.

## Operações

Prefixo público: `/usuarios/api/v1`. Todos os endpoints exigem um JWT válido, audiência `bibliogest-usuarios` e a role **ADMINISTRADOR**. LEITOR e BIBLIOTECARIO não recebem acesso administrativo.

| Método | Caminho                                | Operação                                                             |
| ------ | -------------------------------------- | -------------------------------------------------------------------- |
| GET    | `/usuarios?busca=&pagina=0&tamanho=20` | Buscar contas humanas; página começa em zero, tamanho máximo 20      |
| GET    | `/usuarios/{id}`                       | Consultar perfil, status, função e proteção da conta                 |
| POST   | `/usuarios`                            | Criar leitor com `nome`, `sobrenome`, `email`, `senhaTemporaria`     |
| PUT    | `/usuarios/{id}/bibliotecario`         | Corpo `{"valor":true}` promove; `false` remove somente BIBLIOTECARIO |
| PUT    | `/usuarios/{id}/ativo`                 | Ativar ou desativar com `{"valor":true/false}`                       |
| POST   | `/usuarios/{id}/encerrar-sessoes`      | Encerrar sessões no Keycloak                                         |
| GET    | `/auditoria?pagina=0&tamanho=20`       | Consultar ações administrativas                                      |

A senha inicial tem entre 12 e 128 caracteres e deve ser trocada no primeiro acesso. A criação depende da política de senha do realm e da role padrão LEITOR já configurada no Keycloak. A senha não aparece na resposta nem na auditoria. O painel não oferece redefinição de senha, exclusão de usuários, criação de administradores, impersonação ou edição de clientes/realms.

A própria conta do operador, contas técnicas, usuários com ADMINISTRADOR e usuários com permissões administrativas em `realm-management` são protegidos contra alterações pelo painel. Identidades e roles do operador vêm do JWT validado, não do corpo da requisição. Criar novos administradores continua sendo uma operação feita diretamente no Keycloak.

## Keycloak e credenciais

O cliente confidencial `bibliogest-usuarios-admin` usa client credentials e permissões granulares V2 do Keycloak. Ele recebe a role de consulta `query-users`, permissões de visualização/gerenciamento de usuários e permissão de mapear **somente BIBLIOTECARIO**. Não recebe `manage-users`, `realm-admin`, `manage-clients` nem `impersonation`, que ampliariam ou contornariam as regras granulares.

O cliente público `bibliogest-react` pode incluir ADMINISTRADOR no token apenas quando a conta possui essa role; isso não concede a role a outros usuários. Um audience mapper acrescenta `bibliogest-usuarios` aos access tokens. O client secret administrativo existe apenas no backend, por referência ao Secret Kubernetes `usuarios-keycloak`.

Os endpoints externos são fixos; IDs de usuário são UUIDs e não determinam host, realm ou operação arbitrária. O cliente HTTP tem timeouts e não segue redirecionamentos. Respostas devolvem somente campos permitidos; erros do provedor não expõem seu conteúdo interno.

Ao desativar uma conta ou alterar BIBLIOTECARIO, suas sessões são encerradas. JWTs já emitidos e validados offline pelas APIs podem continuar válidos até expirar (atualmente até cinco minutos); encerrar sessões impede a continuidade via refresh token, mas não promete revogação instantânea desses JWTs.

## Auditoria

Antes de uma alteração externa, a API grava uma intenção PENDENTE com operador, ação, alvo e data. Após a resposta do Keycloak, grava SUCESSO ou NAO_CONFIRMADA. NAO_CONFIRMADA pode representar falha ou execução parcial; um timeout após uma alteração não permite concluir que nada foi alterado. PENDENTE pode indicar uma interrupção ou falha ao registrar o resultado. Em ambos os casos, confira o Keycloak antes de repetir a ação.

O PostgreSQL deve estar disponível para registrar a intenção antes da chamada externa. Não existe transação distribuída entre banco e Keycloak. Dados de formulário, senhas, access tokens e client secrets não são registrados. A migration Flyway `V1__create_auditoria.sql` cria a tabela e seu índice. A lista de auditoria é restrita a administradores.

## Desenvolvimento

Copie `.env.example` para `.env` e preencha credenciais de um ambiente de desenvolvimento isolado. Não use o cliente administrativo de produção para testes locais. A configuração de execução do IntelliJ pode carregar o arquivo. Pelo terminal:

```bash
set -a
source .env
set +a
./mvnw spring-boot:run
```

Use PostgreSQL próprio para desenvolvimento. O Swagger local está em `http://localhost:8082/usuarios/swagger-ui/index.html`; use Authorize com um access token de administrador. Swagger fica desligado em produção. A saúde de prontidão está em `/usuarios/actuator/health/readiness`.

## Testes e CI

```bash
./mvnw --batch-mode --no-transfer-progress clean verify
```

A suíte usa PostgreSQL real via Testcontainers (Docker necessário), MockMvc e um provedor HTTP simulado. Cobre autorização por role, claims inválidas, criação, status, proteção de contas, mapeamento da função permitida, auditoria e respostas inválidas ou indisponibilidade do provedor. Os testes não dependem de senhas ou contas reais do sistema.

Checkstyle verifica o código; JaCoCo exige ao menos **80% de linhas e branches**, sem excluir services ou controllers. O CI usa `needs`: lint → testes/cobertura → empacotamento. Relatórios HTML e JUnit ficam em `target/site/jacoco/` e `target/surefire-reports/`, e no artefato `coverage-and-tests` do GitHub Actions. O resumo do job contém os percentuais.

## Produção

O Dockerfile usa build Maven e runtime Java 21 sem root. O pod usa filesystem somente leitura, `/tmp` efêmero e não recebe token da ServiceAccount Kubernetes. Credenciais do banco vêm de `usuarios-db`; o segredo do cliente vem de `usuarios-keycloak`. IDs públicos de roles/clientes ficam em `usuarios-config`. O Ingress encaminha apenas `/usuarios/api` para o Service interno. NetworkPolicies autorizam entrada pelo Traefik e acesso do serviço ao PostgreSQL e Keycloak.

O banco `bibliogest_usuarios` possui um usuário próprio com o mesmo nome. Além da NetworkPolicy, o `postgres-hba` precisa permitir esse par banco/usuário com `scram-sha-256` na rede dos pods (`10.42.0.0/16` neste cluster), antes da regra final de rejeição. Após atualizar o ConfigMap, aguarde a projeção no volume e execute `SELECT pg_reload_conf()` por uma conexão administrativa local; não é necessário reiniciar o PostgreSQL. A regra está no manifesto `deploy/k8s/10-postgres.yaml` do Acervo, que mantém os recursos compartilhados.

O frontend utiliza `/usuarios/api` na mesma origem, portanto não há liberação genérica de CORS. A CSP continua sendo fornecida pelo Nginx do frontend. `.env`, dumps, chaves privadas, `target` e README não entram na imagem. Faça backup do Keycloak antes de configurar clientes/policies e preserve o banco de auditoria entre atualizações.
