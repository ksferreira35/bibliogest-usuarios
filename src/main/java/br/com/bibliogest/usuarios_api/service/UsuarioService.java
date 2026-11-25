package br.com.bibliogest.usuarios_api.service;

import br.com.bibliogest.usuarios_api.client.KeycloakClient;
import br.com.bibliogest.usuarios_api.dto.CriarUsuarioRequest;
import br.com.bibliogest.usuarios_api.dto.UsuarioResponse;
import br.com.bibliogest.usuarios_api.repository.AuditoriaRepository;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UsuarioService {
  private final KeycloakClient keycloak;
  private final AuditoriaRepository audit;

  public UsuarioService(KeycloakClient keycloak, AuditoriaRepository audit) {
    this.keycloak = keycloak;
    this.audit = audit;
  }

  public List<UsuarioResponse> listar(String busca, int pagina, int tamanho) {
    return call(t -> keycloak.listar(t, busca, pagina * tamanho, tamanho));
  }

  public UsuarioResponse buscar(String id) {
    return call(t -> keycloak.buscar(t, id));
  }

  public UsuarioResponse criar(String ator, CriarUsuarioRequest request) {
    return alterar(ator, "CRIAR_CONTA", null, t -> keycloak.criar(t, request));
  }

  public void alterarConta(String ator, String id, String acao, boolean valor) {
    if (ator.equals(id))
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Sua própria conta deve ser administrada no Keycloak.");
    alterar(
        ator,
        acao,
        id,
        t -> {
          var target = keycloak.buscar(t, id);
          if (Boolean.TRUE.equals(target.protegido()))
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Contas administrativas ou técnicas são gerenciadas somente no Keycloak.");
          switch (acao) {
            case "BIBLIOTECARIO" -> {
              keycloak.bibliotecario(t, id, valor);
              keycloak.logout(t, id);
            }
            case "ATIVAR" -> {
              keycloak.ativo(t, id, valor);
              if (!valor) keycloak.logout(t, id);
            }
            case "ENCERRAR_SESSOES" -> keycloak.logout(t, id);
            default -> throw new IllegalArgumentException("Operação inválida.");
          }
          return null;
        });
  }

  public List<Map<String, Object>> auditoria(int pagina, int tamanho) {
    return audit.listar(pagina, tamanho);
  }

  private <T> T alterar(String ator, String acao, String alvo, Function<String, T> action) {
    var id = audit.iniciar(ator, acao, alvo);
    try {
      T result = call(action);
      audit.concluir(id, true, result instanceof UsuarioResponse user ? user.id() : alvo);
      return result;
    } catch (RuntimeException error) {
      audit.concluir(id, false, alvo);
      throw error;
    }
  }

  private <T> T call(Function<String, T> action) {
    try {
      return action.apply(keycloak.token());
    } catch (RestClientResponseException error) {
      throw KeycloakClient.translate(error);
    } catch (RestClientException error) {
      throw KeycloakClient.unavailable();
    }
  }
}
