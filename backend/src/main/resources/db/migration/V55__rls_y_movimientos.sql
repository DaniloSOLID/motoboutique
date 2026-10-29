-- Mantiene disponibles las tablas a SQL/JPA con el rol postgres y bloquea REST de Supabase.
-- Se excluye flyway_schema_history: Flyway la tiene bloqueada mientras migra.
DO $$
DECLARE objeto record;
BEGIN
 FOR objeto IN SELECT tablename FROM pg_tables
               WHERE schemaname = 'public' AND tablename <> 'flyway_schema_history'
 LOOP
  EXECUTE format('ALTER TABLE public.%I ENABLE ROW LEVEL SECURITY', objeto.tablename);
 END LOOP;
END;
$$;

CREATE OR REPLACE FUNCTION aplicar_movimiento_stock()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE saldo_actual NUMERIC(14,3);
BEGIN
 INSERT INTO stock(producto_id, almacen_id, cantidad)
 VALUES (NEW.producto_id, NEW.almacen_id, 0)
 ON CONFLICT(producto_id, almacen_id) DO NOTHING;

 SELECT cantidad INTO saldo_actual FROM stock WHERE producto_id=NEW.producto_id AND almacen_id=NEW.almacen_id FOR UPDATE;
 IF NEW.tipo IN ('ENTRADA','TRANSFERENCIA_ENTRADA','AJUSTE_POSITIVO','RECEPCION') THEN
  UPDATE stock SET cantidad=cantidad+NEW.cantidad WHERE producto_id=NEW.producto_id AND almacen_id=NEW.almacen_id;
  SELECT cantidad INTO saldo_actual FROM stock WHERE producto_id=NEW.producto_id AND almacen_id=NEW.almacen_id;
 ELSIF NEW.tipo IN ('SALIDA','TRANSFERENCIA_SALIDA','AJUSTE_NEGATIVO') THEN
  IF saldo_actual < NEW.cantidad THEN RAISE EXCEPTION 'Stock insuficiente para producto % en almacén %', NEW.producto_id, NEW.almacen_id; END IF;
  UPDATE stock SET cantidad=cantidad-NEW.cantidad WHERE producto_id=NEW.producto_id AND almacen_id=NEW.almacen_id;
  SELECT cantidad INTO saldo_actual FROM stock WHERE producto_id=NEW.producto_id AND almacen_id=NEW.almacen_id;
 ELSE
  RAISE EXCEPTION 'Tipo de movimiento no soportado: %', NEW.tipo;
 END IF;
 NEW.saldo_cantidad := saldo_actual;
 RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_aplicar_movimiento_stock ON movimientos_inventario;
CREATE TRIGGER trg_aplicar_movimiento_stock
BEFORE INSERT ON movimientos_inventario
FOR EACH ROW EXECUTE FUNCTION aplicar_movimiento_stock();

CREATE INDEX IF NOT EXISTS idx_transferencias_solicitante ON transferencias(solicitante_id);
CREATE INDEX IF NOT EXISTS idx_conteos_responsable ON conteos_fisicos(responsable_id);
CREATE INDEX IF NOT EXISTS idx_ajustes_solicitante ON ajustes_inventario(solicitante_id);
CREATE INDEX IF NOT EXISTS idx_ajustes_aprobador ON ajustes_inventario(aprobador_id);