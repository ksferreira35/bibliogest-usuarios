---
name: usuarios-deploy-docker
description: "Manter Docker, manifests de Usuários, prontidão e referências de configuração de produção."
---

# Usuarios Deploy Docker

- Consulte Dockerfile e deploy/k8s/usuarios.yaml. Runtime Java 21 sem root, filesystem read-only, /tmp efêmero e nenhuma montagem de token de ServiceAccount.
- ConfigMap fornece IDs de configuração; Secrets fornecem valores de credenciais. Preserve Service interno, NetworkPolicies de entrada e comunicação PostgreSQL/Keycloak.
- Ingress do conjunto encaminha /usuarios/api. O PostgreSQL mantém isolamento por usuário/banco e a auditoria persistente. Não altere configurações do Keycloak ou banco como efeito colateral de testes.
- Publicação autorizada deve validar versão, rollout e prontidão. Preserve credenciais, backups e capacidade de reversão; Swagger permanece desligado em produção.

## Fluxo de trabalho e critérios de entrega

- Preserve isolamento do pod, referências de credenciais e banco que guarda auditoria. Não inclua segredos nos manifests ou build.
- Valide imagem local, recursos e readiness antes de publicar. Alterações do Keycloak e políticas de produção são operações separadas.
- Após atualização autorizada, confira saúde e consultas permitidas; mantenha dados e possibilidade de reversão.

## Referência do projeto

Consulte [contexto técnico](references/contexto.md) para Arquitetura do BiblioGest, Testes e CI. Use o [README do repositório](../../../README.md) e o código da branch atual como fontes de verdade.
