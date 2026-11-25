package br.com.bibliogest.usuarios_api.repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AuditoriaRepository {
  private final JdbcTemplate jdbc;

  public AuditoriaRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public UUID iniciar(String ator, String acao, String alvo) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO auditoria(id,ator,acao,alvo,estado,criada_em) VALUES"
            + " (?,?,?,?,'PENDENTE',CURRENT_TIMESTAMP)",
        id,
        ator,
        acao,
        alvo);
    return id;
  }

  public void concluir(UUID id, boolean sucesso, String alvo) {
    jdbc.update(
        "UPDATE auditoria SET estado=?,alvo=COALESCE(?,alvo),concluida_em=CURRENT_TIMESTAMP WHERE"
            + " id=?",
        sucesso ? "SUCESSO" : "NAO_CONFIRMADA",
        alvo,
        id);
  }

  public List<Map<String, Object>> listar(int pagina, int tamanho) {
    return jdbc.queryForList(
        "SELECT id,ator,acao,alvo,estado,criada_em,concluida_em FROM auditoria ORDER BY criada_em"
            + " DESC,id LIMIT ? OFFSET ?",
        tamanho,
        pagina * tamanho);
  }
}
