---
name: usuarios-spring-boot
description: "Implementar controllers, DTOs, serviços e tratamento de erros Java/Spring Boot da API de usuarios."
---

# Usuarios Spring Boot

- Java 21, Maven, Spring Boot. Consulte `pom.xml` e o código em `src/main/java/br/com/bibliogest/usuarios_api/`. Respeite a separação controller, DTO, service e repository; não exponha entidades diretamente como contrato.
- Receba entradas tipadas e validadas, preserve códigos HTTP e erros padronizados do ApiExceptionHandler. Mantenha regras de negócio no service e consultas parametrizadas no repository.
- Consulte `application.yaml` e README para perfil local, configuração de banco e prefixo /usuarios/api/v1. Swagger/OpenAPI fica habilitado localmente, desligado em produção. Actuator fornece prontidão.
- Coordene mudanças de contratos com consumidores; use commits pequenos e não altere os outros serviços sem necessidade.
