# Contexto técnico — usuarios-contas-keycloak

Trechos temáticos do README da main. Confira o código e o README vigente se o contrato tiver mudado.

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
