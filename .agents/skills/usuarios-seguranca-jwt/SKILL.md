---
name: usuarios-seguranca-jwt
description: "Manter autenticação e autorização defensiva Spring Security/Keycloak na API de usuarios."
---

# Usuarios Seguranca Jwt

- Consulte `config/SecurityConfiguration.java` e teste de roles. A API valida JWT Bearer, emissor, assinatura, audiência bibliogest-usuarios e validade; converta apenas roles reconhecidas.
- Todos os endpoints administrativos exigem ADMINISTRADOR, sem herdar esse acesso por LEITOR ou BIBLIOTECARIO.
- Preserve controles de autorização no backend mesmo se a interface esconder comandos. Valide entradas, parametrização SQL, mensagens de erro sem dados internos e acesso a registros de acordo com o perfil.
- Use variáveis e referências a Secrets para credenciais; não exponha JWTs ou segredos em logs, documentação ou testes. Não habilite Swagger em produção sem decisão explícita.

## Fluxo de trabalho e critérios de entrega

- Confira perfil funcional, ownership quando aplicável e validação do JWT no backend. Os testes de claims devem usar dados simulados.
- Verifique se mudanças em rotas preservam autorização e códigos HTTP esperados. Nunca amplie permissões só para obter um teste aprovado.
- Valide privacidade das respostas, parametrização de consultas e ausência de credenciais em logs, commits e exemplos.

## Referência do projeto

Consulte [contexto técnico](references/contexto.md) para Matriz de permissões, Segurança em camadas. Use o [README do repositório](../../../README.md) e o código da branch atual como fontes de verdade.
