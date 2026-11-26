---
name: usuarios-postgres-flyway
description: "Manter persistência PostgreSQL, schema e migrations Flyway da API de usuarios."
---

# Usuarios Postgres Flyway

- Leia `src/main/resources/db/migration/`, `application.yaml` e os repositories. Use banco próprio de desenvolvimento, credenciais via ambiente e consultas parametrizadas.
- Spring JDBC/JdbcTemplate: banco registra auditoria (ator, ação, alvo, estado e datas), e não senhas ou cópias do cadastro Keycloak.
- Novas mudanças usam migrations incrementais; não altere migrations aplicadas. Preserve dados anteriores e teste evolução de schema com banco isolado.
- Confira resultados de Flyway e prontidão da API. Nunca use produção como banco de teste; backup e alterações operacionais exigem autorização própria.
