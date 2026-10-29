-- Estrutura de treino, sem dados, para uso exclusivo no banco de testes.
SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;
CREATE EXTENSION IF NOT EXISTS unaccent WITH SCHEMA public;
CREATE SCHEMA treino;
CREATE TYPE treino.tipo_veiculo AS ENUM (
    'CARRO',
    'MOTO'
);
CREATE FUNCTION treino.auditar_cadastro() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER
    SET search_path TO 'pg_catalog', 'treino'
    AS $$
BEGIN
 INSERT INTO treino.historico(entidade,registro_id,operacao,antes,depois)
 VALUES(TG_TABLE_NAME,NEW.id,CASE WHEN TG_OP='INSERT' THEN 'CADASTRO'
 WHEN NEW.excluido_em IS NOT NULL AND OLD.excluido_em IS NULL THEN 'EXCLUSAO_LOGICA' ELSE 'ALTERACAO' END,
 CASE WHEN TG_OP='INSERT' THEN NULL ELSE to_jsonb(OLD) END,to_jsonb(NEW));
 IF TG_TABLE_NAME='proprietario' AND NEW.excluido_em IS NOT NULL AND OLD.excluido_em IS NULL THEN
  UPDATE treino.veiculo SET excluido_em=NEW.excluido_em WHERE id_proprietario=NEW.id AND excluido_em IS NULL;
 END IF;
 RETURN NEW;
END $$;
CREATE FUNCTION treino.consultar_placa(p_placa text) RETURNS TABLE(id_veiculo integer, placa character varying, renavam character varying, chassi character varying, ano_fabricacao integer, tipo text, id_proprietario integer, nome_proprietario character varying, cpf_cnpj character varying, rua_proprietario character varying, numero_proprietario character varying, cep_proprietario character varying, cidade_proprietario character varying, estado_proprietario character varying, complemento_proprietario character varying)
    LANGUAGE sql STABLE
    AS $$
    SELECT v.id, v.placa, v.renavam, v.chassi, v.ano_fabricacao,
           v.tipo::text, p.id, p.nome, p.cpf_cnpj, p.rua, p.numero, p.cep,
           p.cidade, p.estado, p.complemento
    FROM treino.veiculo AS v
    JOIN treino.proprietario AS p ON p.id = v.id_proprietario
    WHERE v.excluido_em IS NULL AND p.excluido_em IS NULL AND v.placa = upper(replace(btrim(p_placa), '-', ''))
$$;
CREATE FUNCTION treino.normalizar_busca(valor text) RETURNS text
    LANGUAGE sql STABLE PARALLEL SAFE
    AS $$
    SELECT lower(public.unaccent('public.unaccent'::regdictionary, coalesce(valor, '')))
$$;
CREATE FUNCTION treino.pkg_proprietario___registrar_log() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER
    SET search_path TO 'pg_catalog', 'treino'
    AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        INSERT INTO treino.log_proprietario
            (id_proprietario, cpf_cnpj, data_criacao)
        VALUES (NEW.id, NEW.cpf_cnpj, CURRENT_TIMESTAMP);
    ELSIF TG_OP = 'UPDATE' THEN
        -- Também cobre proprietários cadastrados antes da instalação do trigger.
        -- Nesse caso, data_criacao registra o início do histórico disponível.
        INSERT INTO treino.log_proprietario
            (id_proprietario, cpf_cnpj, data_criacao, data_alteracao)
        VALUES (NEW.id, NEW.cpf_cnpj, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        ON CONFLICT (id_proprietario) DO UPDATE
            SET cpf_cnpj = EXCLUDED.cpf_cnpj,
                data_alteracao = EXCLUDED.data_alteracao;
    END IF;
    RETURN NEW;
END;
$$;
CREATE FUNCTION treino.proteger_cadastro() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE dono bigint;
BEGIN
 IF TG_OP = 'DELETE' THEN RAISE EXCEPTION 'Exclusão física proibida. Utilize exclusão lógica.' USING ERRCODE='23514'; END IF;
 IF TG_OP = 'UPDATE' AND OLD.excluido_em IS NOT NULL THEN
  RAISE EXCEPTION 'Registro inativo não pode ser alterado.' USING ERRCODE='23514';
 END IF;
 IF TG_TABLE_NAME = 'proprietario' THEN
  NEW.cpf_cnpj=upper(NEW.cpf_cnpj);
  IF TG_OP = 'UPDATE' AND NEW.cpf_cnpj IS DISTINCT FROM OLD.cpf_cnpj THEN
   RAISE EXCEPTION 'CPF/CNPJ não pode ser alterado após o cadastro.' USING ERRCODE='23514';
  END IF;
  NEW.nome=upper(NEW.nome); NEW.rua=upper(NEW.rua); NEW.numero=upper(NEW.numero);
  NEW.cep=upper(NEW.cep); NEW.cidade=upper(NEW.cidade); NEW.estado=upper(NEW.estado);
  NEW.complemento=upper(NEW.complemento);
 ELSE
  NEW.placa=upper(NEW.placa); NEW.renavam=upper(NEW.renavam); NEW.chassi=upper(NEW.chassi);
  IF NEW.excluido_em IS NULL THEN
   SELECT id INTO dono FROM treino.proprietario WHERE id=NEW.id_proprietario AND excluido_em IS NULL FOR UPDATE;
   IF dono IS NULL THEN RAISE EXCEPTION 'Selecione um proprietário ativo.' USING ERRCODE='23503'; END IF;
  END IF;
 END IF;
 RETURN NEW;
END $$;
CREATE FUNCTION treino.proteger_historico() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN RAISE EXCEPTION 'Histórico é imutável.' USING ERRCODE='23514'; END $$;
SET default_tablespace = '';
SET default_table_access_method = heap;
CREATE TABLE treino.historico (
    id bigint NOT NULL,
    entidade text NOT NULL,
    registro_id bigint NOT NULL,
    operacao text NOT NULL,
    ocorrido_em timestamp with time zone DEFAULT clock_timestamp() NOT NULL,
    usuario_banco text DEFAULT SESSION_USER NOT NULL,
    antes jsonb,
    depois jsonb
);
CREATE SEQUENCE treino.historico_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
ALTER SEQUENCE treino.historico_id_seq OWNED BY treino.historico.id;
CREATE TABLE treino.log_proprietario (
    id integer NOT NULL,
    id_proprietario integer NOT NULL,
    cpf_cnpj character varying(14) NOT NULL,
    data_criacao timestamp(0) without time zone NOT NULL,
    data_alteracao timestamp(0) without time zone
);
CREATE SEQUENCE treino.log_proprietario_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
ALTER SEQUENCE treino.log_proprietario_id_seq OWNED BY treino.log_proprietario.id;
CREATE TABLE treino.proprietario (
    id integer NOT NULL,
    cpf_cnpj character varying(14) NOT NULL,
    nome character varying(150) NOT NULL,
    rua character varying(150),
    numero character varying(20),
    cep character varying(8),
    cidade character varying(100),
    estado character varying(2),
    complemento character varying(255),
    excluido_em timestamp with time zone
);
CREATE SEQUENCE treino.proprietario_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
ALTER SEQUENCE treino.proprietario_id_seq OWNED BY treino.proprietario.id;
CREATE TABLE treino.veiculo (
    id integer NOT NULL,
    placa character varying(7) NOT NULL,
    renavam character varying(11) NOT NULL,
    chassi character varying(17) NOT NULL,
    ano_fabricacao integer NOT NULL,
    id_proprietario integer NOT NULL,
    tipo treino.tipo_veiculo DEFAULT 'CARRO'::treino.tipo_veiculo NOT NULL,
    excluido_em timestamp with time zone
);
CREATE SEQUENCE treino.veiculo_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
ALTER SEQUENCE treino.veiculo_id_seq OWNED BY treino.veiculo.id;
ALTER TABLE ONLY treino.historico ALTER COLUMN id SET DEFAULT nextval('treino.historico_id_seq'::regclass);
ALTER TABLE ONLY treino.log_proprietario ALTER COLUMN id SET DEFAULT nextval('treino.log_proprietario_id_seq'::regclass);
ALTER TABLE ONLY treino.proprietario ALTER COLUMN id SET DEFAULT nextval('treino.proprietario_id_seq'::regclass);
ALTER TABLE ONLY treino.veiculo ALTER COLUMN id SET DEFAULT nextval('treino.veiculo_id_seq'::regclass);
ALTER TABLE ONLY treino.historico
    ADD CONSTRAINT historico_pkey PRIMARY KEY (id);
ALTER TABLE ONLY treino.log_proprietario
    ADD CONSTRAINT log_proprietario_id_proprietario_key UNIQUE (id_proprietario);
ALTER TABLE ONLY treino.log_proprietario
    ADD CONSTRAINT log_proprietario_pkey PRIMARY KEY (id);
ALTER TABLE ONLY treino.proprietario
    ADD CONSTRAINT proprietario_cpf_cnpj_key UNIQUE (cpf_cnpj);
ALTER TABLE ONLY treino.proprietario
    ADD CONSTRAINT proprietario_pkey PRIMARY KEY (id);
ALTER TABLE ONLY treino.veiculo
    ADD CONSTRAINT veiculo_chassi_key UNIQUE (chassi);
ALTER TABLE ONLY treino.veiculo
    ADD CONSTRAINT veiculo_pkey PRIMARY KEY (id);
ALTER TABLE ONLY treino.veiculo
    ADD CONSTRAINT veiculo_placa_key UNIQUE (placa);
ALTER TABLE ONLY treino.veiculo
    ADD CONSTRAINT veiculo_renavam_key UNIQUE (renavam);
CREATE INDEX historico_data ON treino.historico USING btree (ocorrido_em DESC, id DESC);
CREATE INDEX historico_entidade_registro ON treino.historico USING btree (entidade, registro_id, ocorrido_em DESC);
CREATE INDEX idx_veiculo_id_proprietario ON treino.veiculo USING btree (id_proprietario);
CREATE INDEX proprietario_ativo_nome ON treino.proprietario USING btree (nome) WHERE (excluido_em IS NULL);
CREATE INDEX veiculo_ativo_proprietario ON treino.veiculo USING btree (id_proprietario) WHERE (excluido_em IS NULL);
CREATE TRIGGER auditar_proprietario AFTER INSERT OR UPDATE ON treino.proprietario FOR EACH ROW EXECUTE FUNCTION treino.auditar_cadastro();
CREATE TRIGGER auditar_veiculo AFTER INSERT OR UPDATE ON treino.veiculo FOR EACH ROW EXECUTE FUNCTION treino.auditar_cadastro();
CREATE TRIGGER impedir_truncate_historico BEFORE TRUNCATE ON treino.historico FOR EACH STATEMENT EXECUTE FUNCTION treino.proteger_historico();
CREATE TRIGGER impedir_truncate_proprietario BEFORE TRUNCATE ON treino.proprietario FOR EACH STATEMENT EXECUTE FUNCTION treino.proteger_historico();
CREATE TRIGGER impedir_truncate_veiculo BEFORE TRUNCATE ON treino.veiculo FOR EACH STATEMENT EXECUTE FUNCTION treino.proteger_historico();
CREATE TRIGGER proteger_historico BEFORE DELETE OR UPDATE ON treino.historico FOR EACH ROW EXECUTE FUNCTION treino.proteger_historico();
CREATE TRIGGER proteger_proprietario BEFORE INSERT OR DELETE OR UPDATE ON treino.proprietario FOR EACH ROW EXECUTE FUNCTION treino.proteger_cadastro();
CREATE TRIGGER proteger_veiculo BEFORE INSERT OR DELETE OR UPDATE ON treino.veiculo FOR EACH ROW EXECUTE FUNCTION treino.proteger_cadastro();
CREATE TRIGGER trg_proprietario_log AFTER INSERT OR UPDATE ON treino.proprietario FOR EACH ROW EXECUTE FUNCTION treino.pkg_proprietario___registrar_log();
ALTER TABLE ONLY treino.veiculo
    ADD CONSTRAINT veiculo_id_proprietario_fkey FOREIGN KEY (id_proprietario) REFERENCES treino.proprietario(id) ON DELETE RESTRICT;
