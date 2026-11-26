---
name: bibliogest-usuarios
description: "Desenvolver e revisar a API administrativa de usuários e auditoria do BiblioGest, integrada ao Keycloak."
---

# BiblioGest — Usuarios

## Antes de alterar

- Leia o README, o workflow de CI e os arquivos de configuração da branch atual. A main pode estar atrás da versão implantada; confirme a base antes de implementar ou integrar alterações.
- Mantenha os contratos REST, os códigos HTTP e a autorização existentes; alterações de contrato precisam ser coordenadas com os consumidores.
- Preserve segredos em variáveis de ambiente ou referências a Secrets. Use valores fictícios nos exemplos e testes; não registre tokens, senhas ou dumps.
- Deploy em RKE2: valide os manifests, use imagens com tags identificáveis e confira prontidão após publicação autorizada. Não modifique produção ao desenvolver ou testar localmente.

## Domínio de usuários

- O Keycloak é a fonte de identidade; PostgreSQL guarda somente auditoria. Não duplique senhas nem cadastro de usuários no banco.
- Todas as operações administrativas exigem ADMINISTRADOR. Preserve proteção da própria conta, contas técnicas, administradores e contas protegidas.
- A API oferece criação de leitor, gestão de BIBLIOTECARIO, estado da conta, encerramento de sessões e auditoria. Preserve o escopo limitado do cliente confidencial e não amplie permissões administrativas como solução de erro.
- Grave intenção de auditoria antes de mutações externas e registre o resultado sem credenciais. Em falha incerta, preserve o estado de auditoria e confira o resultado antes de repetir.
- Teste com provedor HTTP simulado e banco isolado; não use o Keycloak de produção para testes de mutação.
## Desenvolvimento e validação

- Use Java 21 e Maven, preserve as camadas controller, DTO, service e repository. Valide entradas e mantenha os erros padronizados.
- Use migrations Flyway incrementais, sem reescrever migrations já aplicadas. Utilize banco isolado para desenvolvimento e Testcontainers quando disponível.
- Mantenha autenticação JWT e autorização no backend. Valide emissor, audiência, expiração e roles conforme o contrato do serviço.
- Execute `./mvnw clean verify` (ou `mvn clean verify`). Quando a branch incluir JaCoCo e Checkstyle, preserve a cobertura mínima de 80% de linhas e branches e as verificações de lint. Swagger somente no perfil local.
