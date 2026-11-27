# Contexto técnico — usuarios-postgres-flyway

Trechos temáticos do README da main. Confira o código e o README vigente se o contrato tiver mudado.

### Modelo de dados

```mermaid
erDiagram
 AUDITORIA {
   UUID id PK
   VARCHAR ator
   VARCHAR acao
   VARCHAR alvo
   VARCHAR estado
   TIMESTAMPTZ criada_em
   TIMESTAMPTZ concluida_em
 }
```

| Coluna | Tipo e finalidade |
| --- | --- |
| id | UUID, PK |
| ator | VARCHAR(64), identificador obrigatório do operador autenticado |
| acao | VARCHAR(40), ação obrigatória |
| alvo | VARCHAR(64), ID opcional da conta atingida |
| estado | VARCHAR(16), obrigatório; CHECK permite PENDENTE, SUCESSO e NAO_CONFIRMADA |
| criada_em | TIMESTAMPTZ, instante obrigatório da intenção |
| concluida_em | TIMESTAMPTZ, instante opcional de conclusão |

O índice ix_auditoria_criada ordena por data e ID. Não há FK para as contas mantidas pelo Keycloak. A migration [V1__create_auditoria.sql](../../../../src/main/resources/db/migration/V1__create_auditoria.sql) cria essa tabela. Não se guarda senha, token ou cadastro duplicado.

### SQL Injection e consistência

AuditoriaRepository usa JdbcTemplate com placeholders `?` nas instruções INSERT/UPDATE e paginação da consulta SELECT. Os valores de ator, ação, alvo, estado e limites são passados como parâmetros, não concatenados a instruções SQL. A validação de JWT exige audiência bibliogest-usuarios e ADMINISTRADOR.

```mermaid
sequenceDiagram
 participant F as Painel administrativo
 participant U as API Usuarios
 participant D as Banco auditoria
 participant K as Keycloak
 F->>U: Operacao autorizada
 U->>D: Registrar intencao PENDENTE
 U->>K: Operacao de conta
 K-->>U: Resultado
 U->>D: Registrar SUCESSO ou NAO_CONFIRMADA
 U-->>F: Resultado da operacao
```

A auditoria não forma transação distribuída com o Keycloak; falhas incertas requerem conferência antes de repetir. Permissões administrativas são limitadas às operações previstas; contas protegidas e o próprio operador não podem ser alterados pelo painel.
