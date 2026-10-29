CREATE TABLE estado_aplicacion (
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
    modulo VARCHAR(60) NOT NULL,
    datos JSONB NOT NULL DEFAULT '{}'::jsonb,
    modificado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (usuario_id, modulo)
);

CREATE INDEX idx_estado_aplicacion_modulo ON estado_aplicacion(modulo);
