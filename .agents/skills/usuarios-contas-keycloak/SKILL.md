---
name: usuarios-contas-keycloak
description: "Implementar listagem, criação de leitores, promoção de bibliotecários, estado e sessões no serviço de Usuários."
---

# Usuarios Contas Keycloak

- Keycloak é fonte de identidade; consulte UsuarioController, UsuarioService, DTOs e KeycloakClient. Listagem paginada com busca e máximo de 20 registros.
- Criar leitor requer nome, sobrenome, email e senha temporária de 12–128 caracteres, trocada no primeiro acesso. Não ofereça redefinição de senha, exclusão, impersonação ou criação de administradores pelo painel.
- Promova/remova somente BIBLIOTECARIO, preserve contas técnicas, administradores e a conta do operador. Valide proteção antes de alterar o estado. Encerrar sessões não promete revogação imediata de JWT já emitido.
- Cliente confidencial possui acesso limitado às tarefas autorizadas; não amplie permissões administrativas para solucionar falhas. Corpos, tokens e segredos não aparecem em logs/respostas. Teste com provedor simulado.

## Fluxo de trabalho e critérios de entrega

- Compare operação, role administrativa e proteção da conta antes de chamar o provedor. A API não oferece promoção a ADMINISTRADOR.
- Preserve a senha temporária apenas no formulário/operação, sem registro em resposta, auditoria ou logs. Nunca use a conta administrativa real em testes.
- Valide busca, paginação, criação, controles permitidos e bloqueio de contas protegidas com provedor simulado.

## Referência do projeto

Consulte [contexto técnico](references/contexto.md) para Operações, Keycloak e credenciais. Use o [README do repositório](../../../README.md) e o código da branch atual como fontes de verdade.
