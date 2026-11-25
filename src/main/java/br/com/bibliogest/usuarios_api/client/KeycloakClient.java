package br.com.bibliogest.usuarios_api.client;

import br.com.bibliogest.usuarios_api.dto.CriarUsuarioRequest;
import br.com.bibliogest.usuarios_api.dto.UsuarioResponse;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class KeycloakClient {
  private final RestClient client;
  private final String realm, clientId, secret, librarianRoleId, managementClientId;
  private static final ParameterizedTypeReference<Map<String, Object>> OBJECT =
      new ParameterizedTypeReference<>() {};
  private static final ParameterizedTypeReference<List<Map<String, Object>>> LIST =
      new ParameterizedTypeReference<>() {};

  public KeycloakClient(
      @Value("${bibliogest.keycloak.base-url}") String base,
      @Value("${bibliogest.keycloak.realm}") String realm,
      @Value("${bibliogest.keycloak.client-id}") String clientId,
      @Value("${bibliogest.keycloak.client-secret}") String secret,
      @Value("${bibliogest.keycloak.librarian-role-id}") String roleId,
      @Value("${bibliogest.keycloak.management-client-id}") String managementClientId) {
    var factory =
        new JdkClientHttpRequestFactory(
            HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build());
    factory.setReadTimeout(Duration.ofSeconds(5));
    client = RestClient.builder().baseUrl(base).requestFactory(factory).build();
    this.realm = realm;
    this.clientId = clientId;
    this.secret = secret;
    this.librarianRoleId = roleId;
    this.managementClientId = managementClientId;
  }

  public String token() {
    var form = new LinkedMultiValueMap<String, String>();
    form.add("grant_type", "client_credentials");
    form.add("client_id", clientId);
    form.add("client_secret", secret);
    try {
      var result =
          client
              .post()
              .uri("/realms/{realm}/protocol/openid-connect/token", realm)
              .body(form)
              .retrieve()
              .body(OBJECT);
      if (result != null && result.get("access_token") instanceof String value && !value.isBlank())
        return value;
    } catch (RestClientException error) {
      throw unavailable();
    }
    throw unavailable();
  }

  private String users() {
    return "/admin/realms/" + realm + "/users";
  }

  public List<UsuarioResponse> listar(String token, String busca, int inicio, int tamanho) {
    var rows =
        client
            .get()
            .uri(
                uri ->
                    uri.path(users())
                        .queryParam("search", busca)
                        .queryParam("first", inicio)
                        .queryParam("max", tamanho)
                        .queryParam("briefRepresentation", false)
                        .build())
            .headers(h -> h.setBearerAuth(token))
            .retrieve()
            .body(LIST);
    if (rows == null) throw unavailable();
    return rows.stream().filter(r -> !technical(r)).map(r -> response(r, null, null)).toList();
  }

  private Map<String, Object> user(String token, String id) {
    var result =
        client
            .get()
            .uri(users() + "/{id}", id)
            .headers(h -> h.setBearerAuth(token))
            .retrieve()
            .body(OBJECT);
    if (result == null) throw unavailable();
    return result;
  }

  private List<Map<String, Object>> roles(String token, String path) {
    var result = client.get().uri(path).headers(h -> h.setBearerAuth(token)).retrieve().body(LIST);
    if (result == null) throw unavailable();
    return result;
  }

  public UsuarioResponse buscar(String token, String id) {
    var row = user(token, id);
    var realmRoles = roles(token, users() + "/" + id + "/role-mappings/realm/composite");
    boolean librarian = realmRoles.stream().anyMatch(r -> "BIBLIOTECARIO".equals(r.get("name")));
    boolean admin = realmRoles.stream().anyMatch(r -> "ADMINISTRADOR".equals(r.get("name")));
    boolean management =
        !roles(
                token,
                users() + "/" + id + "/role-mappings/clients/" + managementClientId + "/composite")
            .isEmpty();
    return response(row, librarian, technical(row) || admin || management);
  }

  public void bibliotecario(String token, String id, boolean enabled) {
    var request =
        client
            .method(
                enabled
                    ? org.springframework.http.HttpMethod.POST
                    : org.springframework.http.HttpMethod.DELETE)
            .uri(users() + "/{id}/role-mappings/realm", id)
            .headers(h -> h.setBearerAuth(token))
            .body(List.of(Map.of("id", librarianRoleId, "name", "BIBLIOTECARIO")));
    request.retrieve().toBodilessEntity();
  }

  public void ativo(String token, String id, boolean enabled) {
    client
        .put()
        .uri(users() + "/{id}", id)
        .headers(h -> h.setBearerAuth(token))
        .body(Map.of("enabled", enabled))
        .retrieve()
        .toBodilessEntity();
  }

  public void logout(String token, String id) {
    client
        .post()
        .uri(users() + "/{id}/logout", id)
        .headers(h -> h.setBearerAuth(token))
        .retrieve()
        .toBodilessEntity();
  }

  public UsuarioResponse criar(String token, CriarUsuarioRequest req) {
    var result =
        client
            .post()
            .uri(users())
            .headers(h -> h.setBearerAuth(token))
            .body(
                Map.of(
                    "username",
                    req.email().strip(),
                    "email",
                    req.email().strip(),
                    "firstName",
                    req.nome().strip(),
                    "lastName",
                    req.sobrenome().strip(),
                    "enabled",
                    true,
                    "emailVerified",
                    false,
                    "credentials",
                    List.of(
                        Map.of(
                            "type",
                            "password",
                            "value",
                            req.senhaTemporaria(),
                            "temporary",
                            true))))
            .retrieve()
            .toBodilessEntity();
    var location = result.getHeaders().getLocation();
    if (location == null) throw unavailable();
    String id = location.getPath().substring(location.getPath().lastIndexOf('/') + 1);
    java.util.UUID.fromString(id);
    return response(user(token, id), false, false);
  }

  public static ResponseStatusException translate(RestClientResponseException error) {
    return switch (error.getStatusCode().value()) {
      case 404 -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta não encontrada.");
      case 409 ->
          new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma conta com esses dados.");
      case 400 ->
          new ResponseStatusException(
              HttpStatus.BAD_REQUEST, "Confira os dados e a política de senha da conta.");
      default -> unavailable();
    };
  }

  public static ResponseStatusException unavailable() {
    return new ResponseStatusException(
        HttpStatus.SERVICE_UNAVAILABLE, "Serviço de contas indisponível. Tente novamente.");
  }

  private static boolean technical(Map<String, Object> row) {
    return row.get("serviceAccountClientId") != null
        || text(row, "username").startsWith("service-account-");
  }

  private static String text(Map<String, Object> row, String key) {
    return row.get(key) instanceof String value ? value : "";
  }

  private static UsuarioResponse response(
      Map<String, Object> row, Boolean librarian, Boolean protectedAccount) {
    return new UsuarioResponse(
        text(row, "id"),
        text(row, "firstName"),
        text(row, "lastName"),
        text(row, "email"),
        Boolean.TRUE.equals(row.get("enabled")),
        librarian,
        protectedAccount);
  }
}
