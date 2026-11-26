---
name: usuarios-contas-keycloak
description: "Implementar listagem, criação de leitores, promoção de bibliotecários, estado e sessões no serviço de Usuários."
---

# Usuarios Contas Keycloak

- Keycloak é fonte de identidade; consulte UsuarioController, UsuarioService, DTOs e KeycloakClient. Listagem paginada com busca e máximo de 20 registros.
- Criar leitor requer nome, sobrenome, email e senha temporária de 12–128 caracteres, trocada no primeiro acesso. Não ofereça redefinição de senha, exclusão, impersonação ou criação de administradores pelo painel.
- Promova/remova somente BIBLIOTECARIO, preserve contas técnicas, administradores e a conta do operador. Valide proteção antes de alterar o estado. Encerrar sessões não promete revogação imediata de JWT já emitido.
- Cliente confidencial possui acesso limitado às tarefas autorizadas; não amplie permissões administrativas para solucionar falhas. Corpos, tokens e segredos não aparecem em logs/respostas. Teste com provedor simulado.
