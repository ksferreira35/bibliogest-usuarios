---
name: usuarios-testes-ci
description: "Validar a API de usuarios com JUnit, MockMvc, Mockito, Checkstyle, JaCoCo e GitHub Actions."
---

# Usuarios Testes Ci

- Execute `./mvnw --batch-mode --no-transfer-progress clean verify` em Java 21. Checkstyle roda em validate, testes em test, cobertura em verify; JaCoCo exige 80% em linhas e branches. Consulte `pom.xml`.
- Use JUnit/MockMvc para resultados, erros e permissões; mocks para integrações externas. Claims de teste são fictícias, e não substituem validação real de JWT em produção.
- Usuários usa PostgreSQL Testcontainers e servidor HTTP simulado; cubra proteção de contas, erros do provedor e auditoria.
- CI: lint → clean verify → package, com needs. Inspecione `target/site/jacoco/index.html` e `target/surefire-reports/`. Não exclua classes nem ignore falhas para atingir o limite.

## Fluxo de trabalho e critérios de entrega

- Identifique o tipo de teste adequado: unitário para regra, MockMvc para contrato e integração isolada para persistência/provedor.
- Cubra o caso esperado, validação, recusa de autorização e erros relevantes. Consulte relatório JaCoCo de branches e linhas e resultados JUnit.
- Mantenha o gate de 80%, sem exclusões artificiais, e verifique que os jobs dependentes do CI continuam condicionados ao sucesso.

## Referência do projeto

Consulte [contexto técnico](references/contexto.md) para Testes e CI. Use o [README do repositório](../../../README.md) e o código da branch atual como fontes de verdade.
