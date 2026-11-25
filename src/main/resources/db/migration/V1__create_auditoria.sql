CREATE TABLE auditoria (
 id UUID PRIMARY KEY, ator VARCHAR(64) NOT NULL, acao VARCHAR(40) NOT NULL,
 alvo VARCHAR(64), estado VARCHAR(16) NOT NULL, criada_em TIMESTAMPTZ NOT NULL,
 concluida_em TIMESTAMPTZ,
 CONSTRAINT ck_auditoria_estado CHECK (estado IN ('PENDENTE','SUCESSO','NAO_CONFIRMADA'))
);
CREATE INDEX ix_auditoria_criada ON auditoria(criada_em DESC, id);
