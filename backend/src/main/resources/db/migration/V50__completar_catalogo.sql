ALTER TABLE modelos_moto ADD COLUMN IF NOT EXISTS cilindrada_cc INTEGER CHECK (cilindrada_cc IS NULL OR cilindrada_cc > 0);
ALTER TABLE modelos_moto ADD COLUMN IF NOT EXISTS tipo VARCHAR(40);
ALTER TABLE productos ADD COLUMN IF NOT EXISTS imagen_url TEXT;
ALTER TABLE productos ADD COLUMN IF NOT EXISTS especificaciones JSONB NOT NULL DEFAULT '{}'::jsonb;
ALTER TABLE stock ADD COLUMN IF NOT EXISTS ubicacion VARCHAR(120);

CREATE INDEX IF NOT EXISTS idx_modelos_moto_cilindrada ON modelos_moto(cilindrada_cc);
CREATE INDEX IF NOT EXISTS idx_modelos_moto_tipo ON modelos_moto(tipo);
CREATE INDEX IF NOT EXISTS idx_productos_codigo ON productos(codigo);
CREATE INDEX IF NOT EXISTS idx_productos_marca ON productos(marca_id);
CREATE INDEX IF NOT EXISTS idx_stock_ubicacion ON stock(ubicacion);
