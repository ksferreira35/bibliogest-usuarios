---
name: usuarios-auditoria
description: "Manter AuditoriaRepository, operações auditadas e registro de falhas parciais da administração de usuários."
---

# Usuarios Auditoria

- Use JdbcTemplate e migration V1__create_auditoria.sql como referência. Registre intenção antes de uma operação externa e resultado depois; ator vem do JWT e alvo do usuário gerenciado.
- Estados PENDENTE, SUCESSO e NAO_CONFIRMADA distinguem intenção, resultado confirmado e incerteza. Não há transação distribuída entre Keycloak e banco; timeout não significa que a alteração falhou.
- Banco indisponível antes da intenção impede alteração externa; em falha parcial preserve o registro e confira estado antes de repetir. Nunca grave senha, token ou corpo integral do formulário.
- Consulta de auditoria é paginada e restrita à role ADMINISTRADOR. Teste sucesso, falhas de integração e indisponibilidade com dados fictícios.

## Fluxo de trabalho e critérios de entrega

- Antes da mutação externa, registre intenção; se isso falhar, não execute a mutação. Preserve ator, ação, alvo, datas e resultado.
- Em resultado incerto, mantenha NAO_CONFIRMADA ou PENDENTE conforme o caso; não repita automaticamente uma operação sem conferência.
- Teste sucesso e falhas de persistência/provedor. Não registre o corpo do formulário, senha ou token.

## Referência do projeto

Consulte [contexto técnico](references/contexto.md) para Auditoria, Modelo de dados, SQL Injection e consistência. Use o [README do repositório](../../../README.md) e o código da branch atual como fontes de verdade.
