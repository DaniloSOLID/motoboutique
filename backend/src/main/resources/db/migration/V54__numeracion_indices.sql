CREATE SEQUENCE IF NOT EXISTS seq_numero_ent;
CREATE SEQUENCE IF NOT EXISTS seq_numero_sal;
CREATE SEQUENCE IF NOT EXISTS seq_numero_trf;
CREATE SEQUENCE IF NOT EXISTS seq_numero_aju;
CREATE SEQUENCE IF NOT EXISTS seq_numero_cnt;
CREATE SEQUENCE IF NOT EXISTS seq_numero_oc;
CREATE SEQUENCE IF NOT EXISTS seq_numero_rec;

CREATE TABLE numeracion_documentos (
 prefijo VARCHAR(3) PRIMARY KEY,
 ultimo_numero BIGINT NOT NULL DEFAULT 0,
 actualizado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
 CONSTRAINT chk_numeracion_prefijo CHECK(prefijo IN ('ENT','SAL','TRF','AJU','CNT','OC','REC')),
 CONSTRAINT chk_numeracion_ultimo CHECK(ultimo_numero >= 0)
);
INSERT INTO numeracion_documentos(prefijo) VALUES ('ENT'),('SAL'),('TRF'),('AJU'),('CNT'),('OC'),('REC') ON CONFLICT(prefijo) DO NOTHING;

CREATE OR REPLACE FUNCTION siguiente_numero(p_prefijo VARCHAR)
RETURNS VARCHAR LANGUAGE plpgsql AS $$
DECLARE v_numero BIGINT;
BEGIN
 UPDATE numeracion_documentos SET ultimo_numero = ultimo_numero + 1, actualizado_en = now()
 WHERE prefijo = p_prefijo RETURNING ultimo_numero INTO v_numero;
 IF NOT FOUND THEN RAISE EXCEPTION 'Prefijo de numeración inválido: %', p_prefijo; END IF;
 RETURN p_prefijo || '-' || lpad(v_numero::text, 6, '0');
END;
$$;

CREATE INDEX IF NOT EXISTS idx_usuarios_nombre ON usuarios(nombre);
CREATE INDEX IF NOT EXISTS idx_usuarios_activo ON usuarios(activo);
CREATE INDEX IF NOT EXISTS idx_modelos_moto_nombre_trgm ON modelos_moto USING GIN(nombre gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_categorias_nombre_trgm ON categorias USING GIN(nombre gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_marcas_nombre_trgm ON marcas USING GIN(nombre gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_movimientos_fecha ON movimientos_inventario(fecha DESC);
CREATE INDEX IF NOT EXISTS idx_ordenes_estado_fecha ON ordenes_compra(estado, fecha DESC);
CREATE INDEX IF NOT EXISTS idx_recepciones_fecha ON recepciones_compra(fecha DESC);
CREATE INDEX IF NOT EXISTS idx_proveedores_activo ON proveedores(activo);
CREATE INDEX IF NOT EXISTS idx_productos_activo ON productos(activo);
CREATE INDEX IF NOT EXISTS idx_stock_producto_almacen ON stock(producto_id, almacen_id);
