package br.com.bibliogest.usuarios_api.dto;

public record UsuarioResponse(
    String id,
    String nome,
    String sobrenome,
    String email,
    boolean ativo,
    Boolean bibliotecario,
    Boolean protegido) {}
