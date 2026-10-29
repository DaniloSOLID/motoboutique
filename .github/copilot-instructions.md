# Moto Boutique: reglas permanentes
Proyecto: control de inventario de una tienda de motos y accesorios (Perú, moneda PEN, formato es-PE). backend/ = Java 21 + Spring Boot 4.1 + Spring Security (sesión + CSRF) + JPA + Flyway + PostgreSQL en Supabase. frontend/ = React 19 + TypeScript + Vite + Tailwind 4. Roles: ADMIN, ALMACENERO, CONTADOR, CONSULTA.

## Reglas generales
- Cero datos de ejemplo en el código: nada de arrays con filas, cifras, nombres ni fechas escritos a mano. Todo dato visible sale de la API y la API lo saca de la base de datos.
- Persistido = guardado en una tabla de PostgreSQL mediante la API, visible tras recargar y desde otro navegador. localStorage no se usa para datos de negocio. Las preferencias de interfaz (tema, tamaño de página, filtros guardados) van a `/api/estado/{modulo}`.
- No inventes endpoints, columnas ni permisos: revisa el código antes. Si falta algo, créalo en la capa correcta.
- Nunca edites migraciones Flyway existentes (V1–V42). Todo cambio de esquema es una migración nueva `V5x__nombre.sql`.
- Nada de secretos en el código ni en archivos versionados.

## Backend
- Capas: web → service → repository → domain. Prohibido JdbcTemplate o SQL en controllers. DTOs como record, MapStruct para mapear, Bean Validation en la entrada, entidades que extienden AuditableEntity, borrado lógico con `eliminado`.
- Cada endpoint lleva @PreAuthorize por permiso (hasAuthority) además de hasRole('ADMIN'). Errores con NegocioException/RecursoNoEncontradoException y el ProblemDetail existente, mensajes en español.
- Listados con parámetros `q`, `page`, `size`, `sort` más filtros propios, respuesta `PageResponse<T>`; búsqueda, filtro, orden y paginación en el servidor.
- Dinero y cantidades con BigDecimal (dinero escala 2, cantidades escala 3).
- El stock solo cambia mediante `MovimientoInventarioService`, dentro de una transacción y con bloqueo de fila. El kardex es solo inserción, el costo es promedio ponderado y el stock nunca es negativo.
- Toda operación nueva lleva pruebas unitarias del servicio y pruebas MockMvc de autorización (200 con el rol correcto, 403 con otro, 401 sin sesión).

## Frontend
- Un archivo por pantalla o componente, carpetas `features/<modulo>/`, `components/`, `api/`, `hooks/`. Ningún archivo supera ~300 líneas.
- Datos del servidor con TanStack Query. Las mutaciones invalidan las consultas relacionadas y muestran un aviso de éxito o error.
- Todo listado tiene buscador con debounce, filtros, orden y paginación enviados al servidor y reflejados en la URL, más estados de carga, vacío y error.
- Todo formulario valida, muestra estado de envío y errores del servidor por campo, y no se cierra si falla.
- Las gráficas muestran datos reales. Sin datos, estado vacío; nunca valores inventados ni mínimos forzados.
- Permisos en la UI por código de permiso (tienePermiso), no por nombre de rol. Se oculta lo no permitido y el backend lo valida otra vez.
- TypeScript estricto, sin any, textos en español, montos en S/ y fechas es-PE.

## Antes de terminar cada tarea
Ejecuta `./mvnw test` y en frontend `npm run build` (y lint y tests si existen), corrige todo y entrega un resumen: archivos creados o modificados, endpoints nuevos, migraciones y cómo probarlo a mano.
