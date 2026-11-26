---
name: usuarios-deploy-docker
description: "Manter Docker, manifests de Usuários, prontidão e referências de configuração de produção."
---

# Usuarios Deploy Docker

- Consulte Dockerfile e deploy/k8s/usuarios.yaml. Runtime Java 21 sem root, filesystem read-only, /tmp efêmero e nenhuma montagem de token de ServiceAccount.
- ConfigMap fornece IDs de configuração; Secrets fornecem valores de credenciais. Preserve Service interno, NetworkPolicies de entrada e comunicação PostgreSQL/Keycloak.
- Ingress do conjunto encaminha /usuarios/api. O PostgreSQL mantém isolamento por usuário/banco e a auditoria persistente. Não altere configurações do Keycloak ou banco como efeito colateral de testes.
- Publicação autorizada deve validar versão, rollout e prontidão. Preserve credenciais, backups e capacidade de reversão; Swagger permanece desligado em produção.
