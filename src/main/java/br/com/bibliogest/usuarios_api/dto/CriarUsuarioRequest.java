package br.com.bibliogest.usuarios_api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CriarUsuarioRequest(
    @NotBlank @Size(max = 100) String nome,
    @NotBlank @Size(max = 100) String sobrenome,
    @NotBlank @Email @Size(max = 254) String email,
    @NotBlank @Size(min = 12, max = 128) String senhaTemporaria) {
  @Override
  public String toString() {
    return "CriarUsuarioRequest[conteudo omitido]";
  }
}
