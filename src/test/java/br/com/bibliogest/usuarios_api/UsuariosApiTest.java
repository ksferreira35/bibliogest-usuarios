package br.com.bibliogest.usuarios_api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = {
      "server.servlet.context-path=/",
      "spring.datasource.url=jdbc:tc:postgresql:17.12-bookworm:///usuarios-test",
      "spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver",
      "spring.datasource.username=test",
      "spring.datasource.password=test",
      "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://login.example.test/realms/bibliogest",
      "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://login.example.test/certs",
      "bibliogest.keycloak.client-secret=not-a-real-secret",
      "bibliogest.keycloak.librarian-role-id=role-bib",
      "bibliogest.keycloak.management-client-id=management"
    })
@AutoConfigureMockMvc
class UsuariosApiTest {
  static final String ACTOR = "00000000-0000-4000-8000-000000000001",
      USER = "00000000-0000-4000-8000-000000000002",
      PROTECTED = "00000000-0000-4000-8000-000000000003";
  static final AtomicInteger failure = new AtomicInteger(),
      tokenMode = new AtomicInteger(),
      bodyMode = new AtomicInteger();
  static volatile boolean librarian, enabled = true;
  static volatile String lastBody = "";
  static final HttpServer server;

  static {
    try {
      server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      server.createContext(
          "/",
          exchange -> {
            String path = exchange.getRequestURI().getPath(), method = exchange.getRequestMethod();
            lastBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String body = "{}";
            int code = 200;
            if (path.endsWith("/token")) {
              if (tokenMode.get() == 1) code = 401;
              body = tokenMode.get() == 2 ? "{}" : "{\"access_token\":\"service-token\"}";
            } else {
              assertThat(exchange.getRequestHeaders().getFirst("Authorization"))
                  .isEqualTo("Bearer service-token");
              code = failure.getAndSet(0);
              if (code == 0) code = 200;
              if (code == 200) {
                if (path.endsWith("/role-mappings/realm/composite"))
                  body =
                      path.contains(PROTECTED)
                          ? "[{\"name\":\"ADMINISTRADOR\"}]"
                          : librarian ? "[{\"name\":\"BIBLIOTECARIO\"}]" : "[]";
                else if (path.contains("/role-mappings/clients/")) body = "[]";
                else if (path.endsWith("/role-mappings/realm")) {
                  assertThat(lastBody).contains("BIBLIOTECARIO").doesNotContain("ADMINISTRADOR");
                  librarian = method.equals("POST");
                  code = 204;
                } else if (path.endsWith("/logout")) code = 204;
                else if (path.endsWith("/users") && method.equals("GET"))
                  body =
                      "["
                          + user(USER, "reader")
                          + ","
                          + user(PROTECTED, "service-account-hidden")
                          + "]";
                else if (path.endsWith("/users") && method.equals("POST")) {
                  code = 201;
                  exchange
                      .getResponseHeaders()
                      .set("Location", "http://localhost/admin/realms/bibliogest/users/" + USER);
                  assertThat(lastBody).contains("temporary").doesNotContain("ADMINISTRADOR");
                } else if (method.equals("PUT")) {
                  enabled = lastBody.contains("true");
                  code = 204;
                } else body = user(USER, "reader");
              }
            }
            if (bodyMode.get() == 1 && !path.endsWith("/token")) body = "null";
            if (tokenMode.get() == 3 && path.endsWith("/token")) body = "{\"access_token\":\"\"}";
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(code, code == 204 ? -1 : bytes.length);
            if (code != 204) exchange.getResponseBody().write(bytes);
            exchange.close();
          });
      server.start();
    } catch (Exception e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  static String user(String id, String username) {
    return "{\"id\":\""
        + id
        + "\",\"username\":\""
        + username
        + "\",\"firstName\":\"Pessoa\",\"lastName\":\"Teste\",\"email\":\"test@example.test\",\"enabled\":"
        + enabled
        + "}";
  }

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry p) {
    p.add(
        "bibliogest.keycloak.base-url", () -> "http://127.0.0.1:" + server.getAddress().getPort());
  }

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @MockitoBean JwtDecoder decoder;

  @BeforeEach
  void setup() {
    failure.set(0);
    tokenMode.set(0);
    bodyMode.set(0);
    librarian = false;
    enabled = true;
    jdbc.update("DELETE FROM auditoria");
    for (String role : List.of("ADMINISTRADOR", "LEITOR", "BIBLIOTECARIO", "admin"))
      when(decoder.decode(role))
          .thenReturn(
              Jwt.withTokenValue(role)
                  .header("alg", "RS256")
                  .subject(ACTOR)
                  .claim("realm_access", Map.of("roles", List.of(role)))
                  .build());
  }

  @Test
  void anonymousRejected() throws Exception {
    mvc.perform(get("/api/v1/usuarios")).andExpect(status().isUnauthorized());
  }

  @ParameterizedTest
  @ValueSource(strings = {"LEITOR", "BIBLIOTECARIO", "admin"})
  void rolesDoNotGrantAdministration(String role) throws Exception {
    mvc.perform(get("/api/v1/usuarios").header("Authorization", "Bearer " + role))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/v1/usuarios")
                .header("Authorization", "Bearer " + role)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void listsOnlyHumanAccountsAndFetchesDetails() throws Exception {
    mvc.perform(
            get("/api/v1/usuarios?busca=Pessoa&pagina=0&tamanho=999")
                .header("Authorization", "Bearer ADMINISTRADOR"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].nome").value("Pessoa"));
    mvc.perform(get("/api/v1/usuarios/" + USER).header("Authorization", "Bearer ADMINISTRADOR"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.protegido").value(false));
  }

  @Test
  void roleChangesAreAuditedAndSessionsEnd() throws Exception {
    for (boolean value : new boolean[] {true, false})
      mvc.perform(
              put("/api/v1/usuarios/" + USER + "/bibliotecario")
                  .header("Authorization", "Bearer ADMINISTRADOR")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"valor\":" + value + "}"))
          .andExpect(status().isNoContent());
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM auditoria WHERE estado='SUCESSO'", Integer.class))
        .isEqualTo(2);
    mvc.perform(get("/api/v1/auditoria").header("Authorization", "Bearer ADMINISTRADOR"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  void enableDisableAndLogout() throws Exception {
    for (boolean value : new boolean[] {false, true})
      mvc.perform(
              put("/api/v1/usuarios/" + USER + "/ativo")
                  .header("Authorization", "Bearer ADMINISTRADOR")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"valor\":" + value + "}"))
          .andExpect(status().isNoContent());
    mvc.perform(
            post("/api/v1/usuarios/" + USER + "/encerrar-sessoes")
                .header("Authorization", "Bearer ADMINISTRADOR"))
        .andExpect(status().isNoContent());
  }

  @Test
  void cannotModifySelfOrProtectedUser() throws Exception {
    mvc.perform(
            put("/api/v1/usuarios/" + ACTOR + "/ativo")
                .header("Authorization", "Bearer ADMINISTRADOR")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"valor\":false}"))
        .andExpect(status().isConflict());
    mvc.perform(
            put("/api/v1/usuarios/" + PROTECTED + "/bibliotecario")
                .header("Authorization", "Bearer ADMINISTRADOR")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"valor\":true}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void createUsesTemporaryPasswordAndRedactsAudit() throws Exception {
    mvc.perform(
            post("/api/v1/usuarios")
                .header("Authorization", "Bearer ADMINISTRADOR")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"nome\":\"Pessoa\",\"sobrenome\":\"Teste\",\"email\":\"test@example.test\",\"senhaTemporaria\":\"temporary-test-only\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.email").value("test@example.test"))
        .andExpect(jsonPath("$.credentials").doesNotExist());
    assertThat(jdbc.queryForList("SELECT * FROM auditoria").toString())
        .doesNotContain("temporary-test-only");
  }

  @ParameterizedTest
  @ValueSource(ints = {400, 404, 409, 500})
  void upstreamFailuresAreSafe(int code) throws Exception {
    failure.set(code);
    mvc.perform(get("/api/v1/usuarios/" + USER).header("Authorization", "Bearer ADMINISTRADOR"))
        .andExpect(status().is(code == 500 ? 503 : code));
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2})
  void invalidServiceToken(int mode) throws Exception {
    tokenMode.set(mode);
    mvc.perform(get("/api/v1/usuarios").header("Authorization", "Bearer ADMINISTRADOR"))
        .andExpect(status().isServiceUnavailable());
  }

  @Test
  void rejectsMalformedAndOversizedInputs() throws Exception {
    mvc.perform(
            get("/api/v1/usuarios?page=0&pagina=-1")
                .header("Authorization", "Bearer ADMINISTRADOR"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            get("/api/v1/usuarios?busca=" + "x".repeat(101))
                .header("Authorization", "Bearer ADMINISTRADOR"))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/api/v1/usuarios/not-uuid").header("Authorization", "Bearer ADMINISTRADOR"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post("/api/v1/usuarios")
                .header("Authorization", "Bearer ADMINISTRADOR")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            put("/api/v1/usuarios/" + USER + "/ativo")
                .header("Authorization", "Bearer ADMINISTRADOR")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void nullUpstreamUserAndListAreUnavailable() throws Exception {
    bodyMode.set(1);
    mvc.perform(get("/api/v1/usuarios/" + USER).header("Authorization", "Bearer ADMINISTRADOR"))
        .andExpect(status().isServiceUnavailable());
    mvc.perform(get("/api/v1/usuarios").header("Authorization", "Bearer ADMINISTRADOR"))
        .andExpect(status().isServiceUnavailable());
  }

  @Test
  void emptyTokenIsNotUsed() throws Exception {
    tokenMode.set(3);
    mvc.perform(get("/api/v1/usuarios").header("Authorization", "Bearer ADMINISTRADOR"))
        .andExpect(status().isServiceUnavailable());
  }

  @Test
  void failedMutationIsAuditedWithoutLeakingProviderDetails() throws Exception {
    failure.set(500);
    mvc.perform(
            post("/api/v1/usuarios/" + USER + "/encerrar-sessoes")
                .header("Authorization", "Bearer ADMINISTRADOR"))
        .andExpect(status().isServiceUnavailable());
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM auditoria WHERE estado='NAO_CONFIRMADA'", Integer.class))
        .isEqualTo(1);
  }
}
