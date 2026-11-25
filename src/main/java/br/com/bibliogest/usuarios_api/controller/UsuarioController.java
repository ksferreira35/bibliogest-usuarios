package br.com.bibliogest.usuarios_api.controller;

import br.com.bibliogest.usuarios_api.dto.CriarUsuarioRequest;
import br.com.bibliogest.usuarios_api.dto.UsuarioResponse;
import br.com.bibliogest.usuarios_api.service.UsuarioService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class UsuarioController {
  private final UsuarioService service;

  public UsuarioController(UsuarioService service) {
    this.service = service;
  }

  public record Flag(@NotNull Boolean valor) {}

  @GetMapping("/usuarios")
  public List<UsuarioResponse> listar(
      @RequestParam(defaultValue = "") String busca,
      @RequestParam(defaultValue = "0") int pagina,
      @RequestParam(defaultValue = "20") int tamanho) {
    if (busca.length() > 100)
      throw new IllegalArgumentException("Busca limitada a 100 caracteres.");
    return service.listar(busca.strip(), page(pagina), size(tamanho));
  }

  @GetMapping("/usuarios/{id}")
  public UsuarioResponse buscar(@PathVariable UUID id) {
    return service.buscar(id.toString());
  }

  @PostMapping("/usuarios")
  public ResponseEntity<UsuarioResponse> criar(
      @Valid @RequestBody CriarUsuarioRequest req, Authentication auth) {
    var user = service.criar(auth.getName(), req);
    return ResponseEntity.created(URI.create("/usuarios/api/v1/usuarios/" + user.id())).body(user);
  }

  @PutMapping("/usuarios/{id}/bibliotecario")
  public ResponseEntity<Void> role(
      @PathVariable UUID id, @Valid @RequestBody Flag flag, Authentication auth) {
    service.alterarConta(auth.getName(), id.toString(), "BIBLIOTECARIO", flag.valor());
    return ResponseEntity.noContent().build();
  }

  @PutMapping("/usuarios/{id}/ativo")
  public ResponseEntity<Void> ativo(
      @PathVariable UUID id, @Valid @RequestBody Flag flag, Authentication auth) {
    service.alterarConta(auth.getName(), id.toString(), "ATIVAR", flag.valor());
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/usuarios/{id}/encerrar-sessoes")
  public ResponseEntity<Void> logout(@PathVariable UUID id, Authentication auth) {
    service.alterarConta(auth.getName(), id.toString(), "ENCERRAR_SESSOES", false);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/auditoria")
  public List<Map<String, Object>> audit(
      @RequestParam(defaultValue = "0") int pagina,
      @RequestParam(defaultValue = "20") int tamanho) {
    return service.auditoria(page(pagina), size(tamanho));
  }

  private static int page(int n) {
    if (n < 0 || n > 100000) throw new IllegalArgumentException("Página inválida.");
    return n;
  }

  private static int size(int n) {
    return Math.max(1, Math.min(20, n));
  }
}
