-- ============================================================
-- V1: Extensiones base de PostgreSQL
-- ============================================================
-- pg_trgm permite búsquedas por texto parcial (ILIKE '%texto%') con
-- índices GIN. Se usará en el catálogo de productos (Fase 2) para
-- buscar rápido por nombre, SKU y descripción entre miles de artículos.

CREATE EXTENSION IF NOT EXISTS pg_trgm;