package br.com.bibliogest.usuarios_api.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.security.oauth2.jwt.Jwt;

class JwtRolesTest {
  static Stream<Object> invalidClaims() {
    return Stream.of(
        "invalid",
        Map.of(),
        Map.of("roles", "BIBLIOTECARIO"),
        Map.of("roles", List.of("admin", "bibliotecario", 42)));
  }

  @ParameterizedTest
  @MethodSource("invalidClaims")
  void malformedRolesNeverGrantPrivileges(Object claim) {
    var jwt =
        Jwt.withTokenValue("test-only").header("alg", "RS256").claim("realm_access", claim).build();
    var authentication = new SecurityConfiguration().jwtAuthenticationConverter().convert(jwt);
    assertThat(authentication.getAuthorities())
        .extracting("authority")
        .containsExactly("FACTOR_BEARER");
  }

  @Test
  void missingRealmDoesNotGrantPrivileges() {
    var jwt = Jwt.withTokenValue("test-only").header("alg", "RS256").subject("reader").build();
    assertThat(
            new SecurityConfiguration().jwtAuthenticationConverter().convert(jwt).getAuthorities())
        .extracting("authority")
        .containsExactly("FACTOR_BEARER");
  }

  @Test
  void onlyExactApplicationRolesAreAccepted() {
    var jwt =
        Jwt.withTokenValue("test-only")
            .header("alg", "RS256")
            .claim(
                "realm_access",
                Map.of("roles", Arrays.asList(null, "LEITOR", "ADMINISTRADOR", "admin")))
            .build();
    assertThat(
            new SecurityConfiguration().jwtAuthenticationConverter().convert(jwt).getAuthorities())
        .extracting("authority")
        .containsExactlyInAnyOrder("FACTOR_BEARER", "ROLE_ADMINISTRADOR");
  }
}
