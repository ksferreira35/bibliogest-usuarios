---
name: usuarios-postgres-flyway
description: "Manter persistência PostgreSQL, schema e migrations Flyway da API de usuarios."
---

# Usuarios Postgres Flyway

- Leia `src/main/resources/db/migration/`, `application.yaml` e os repositories. Use banco próprio de desenvolvimento, credenciais via ambiente e consultas parametrizadas.
- Spring JDBC/JdbcTemplate: banco registra auditoria (ator, ação, alvo, estado e datas), e não senhas ou cópias do cadastro Keycloak.
- Novas mudanças usam migrations incrementais; não altere migrations aplicadas. Preserve dados anteriores e teste evolução de schema com banco isolado.
- Confira resultados de Flyway e prontidão da API. Nunca use produção como banco de teste; backup e alterações operacionais exigem autorização própria.

## Fluxo de trabalho e critérios de entrega

- Compare a entidade/repository com as migrations versionadas; use PKs, FKs e restrições existentes como fonte do contrato de persistência.
- Escreva migration incremental para evolução de esquema e teste com registros anteriores; mantenha parâmetros SQL separados da instrução.
- Valide em banco isolado e revise possíveis efeitos de concorrência, nulabilidade e integridade. Não execute alteração em produção sem autorização.

## Referência do projeto

Consulte [contexto técnico](references/contexto.md) para Modelo de dados, SQL Injection e consistência. Use o [README do repositório](../../../README.md) e o código da branch atual como fontes de verdade.
