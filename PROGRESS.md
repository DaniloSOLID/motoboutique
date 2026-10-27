# PROGRESS — Separación en backend y frontend

Fase: **separar el proyecto en dos aplicaciones independientes** sin perder lo existente.
Origen: https://github.com/mateo-pc/motoboutique (rama `main`, último commit `d31b7ec`). No se crearon ramas ni commits.

## 1. Punto de partida

El repositorio era un monolito Spring Boot con Thymeleaf. Estado real del código:

- **Implementado:** módulo `seguridad` (usuarios, roles, permisos, token de recuperación como entidad, `UsuarioDetailsService`, servicios, DTOs y mappers), auditoría JPA, `DataSeeder`, manejo de errores, `PageResponse`, migraciones `V1` (extensión `pg_trgm`) y `V2` (seguridad).
- **Solo esqueleto (paquetes vacíos con `package-info`):** catalogo, inventario, compras, motos, reportes, configuracion, ventas.
- **Interfaces:** solo existía una plantilla Thymeleaf, `error/general.html`. No había pantalla de login propia (se usaba la generada por Spring Security), ni controllers MVC, ni imágenes, ni CSS propio (Tailwind por CDN).

El diagrama de casos de uso (Sistema de Control de Inventario – Tienda de Motos y Accesorios) define cuatro actores —Administrador, Almacenero, Contador y Consulta— y los paquetes Reportes, Consulta, Administración, Movimientos de inventario, Catálogo y Compras. **Casi todo eso corresponde a fases posteriores**; en esta fase solo se conservaron los cuatro roles que el `SeederSeguridad` ya crea (ADMIN, ALMACENERO, CONTADOR, CONSULTA) y no se desarrolló ningún caso de uso nuevo.

## 2. Arquitectura resultante

```
motoboutique/
├─ backend/    API REST (Spring Boot, puerto 8081)
├─ frontend/   SPA (React + TypeScript + Vite, puerto 5173 en desarrollo)
├─ README.md   cómo ejecutar
└─ PROGRESS.md este documento
```

- **Comunicación:** HTTP + JSON. Autenticación por **sesión** (cookie `JSESSIONID`), la misma que usaba el sistema; no se cambió a JWT para no alterar el modelo de autenticación existente.
- **CSRF:** sigue activo. Cookie `XSRF-TOKEN` (legible por el frontend) + cabecera `X-XSRF-TOKEN` en los métodos que modifican datos.
- **Desarrollo:** el proxy de Vite reenvía `/api` al backend → mismo origen para el navegador. CORS queda configurado (`CORS_ALLOWED_ORIGINS`) para despliegues con orígenes distintos.
- **Errores:** `ProblemDetail` (RFC 9457) para todo, incluidos 401 y 403 generados dentro de la cadena de filtros de seguridad.
- **Capas del backend:** sin cambios (`web → service → repository → domain`, DTOs con MapStruct, paquete base `com.motoboutique`).

### API disponible

| Ruta | Función |
|---|---|
| `POST /api/auth/login` | Inicio de sesión (formulario con `email` y `password`). 204 / 401 |
| `GET /api/auth/me` | Usuario actual con roles y permisos. 200 / 401 |
| `POST /api/auth/logout` | Cierre de sesión. 204 |

### Frontend

- `src/api/`: cliente `fetch` (cookies, CSRF, normalización de `ProblemDetail`), tipos TypeScript de los DTO (`UsuarioDto`, `RolDto`, `PermisoDto`, `PageResponse`) y textos de error espejo de `MensajesHttp`.
- `src/auth/`: `AuthContext` (sesión, `tieneRol`, `tienePermiso`) y `RequireAuth` (ruta protegida).
- `src/pages/`: `ErrorPage` (migración fiel de `error/general.html`), `LoginPage`, `HomePage`.
- Rutas: `/login`, `/` (protegida), `*` → 404.
- Estilos: Tailwind CSS v4 instalado como dependencia (antes por CDN) con las mismas clases, colores (`#f6f5f3`, `#1c1917`, `#c2410c`, `#ea580c`, `#e7e5e4`, `#57534e`) y tipografía del sistema.

## 3. Cambios realizados

**Backend**
1. `pom.xml`: se quitaron `spring-boot-starter-thymeleaf`, `thymeleaf-extras-springsecurity6` y `spring-boot-starter-thymeleaf-test`. Resto de dependencias, Java 21 y procesadores (Lombok, MapStruct) intactos.
2. `SecurityConfig` reescrita para API: login/logout por JSON-friendly handlers (204/401), 401 y 403 en JSON, CSRF por cookie, CORS configurable. Se mantienen `@EnableMethodSecurity`, BCrypt, `UsuarioDetailsService` y el parámetro `email`.
3. Nuevas clases mínimas necesarias para el modelo SPA: `CsrfCookieFilter`, `RespuestasSeguridad`, `AuthController` (`GET /api/auth/me`) y `UsuarioSesionDto`.
4. `ApiExceptionHandler` ahora aplica a toda la aplicación (antes solo a `@RestController`).
5. Eliminados por ser exclusivos de vistas server-side: `GlobalExceptionHandler`, `Flash` y `templates/error/general.html`.
6. `application*.properties`: se quitó la configuración de Thymeleaf; se agregó `app.cors.allowed-origins`; la cookie de sesión de `prod` admite `SESSION_COOKIE_SAME_SITE`.
7. **Seguridad de credenciales:** `application.properties` tenía `spring.datasource.username=postgres` y `spring.datasource.password=admin` escritos en el código, lo que contradecía el README. Ahora usa `${DB_USER:postgres}` y `${DB_PASSWORD}` (sin valor por defecto). Como esa contraseña estuvo en un repositorio público, conviene cambiarla en cualquier PostgreSQL donde se haya usado.
8. `.env.example` ampliado con `CORS_ALLOWED_ORIGINS` y `SESSION_COOKIE_SAME_SITE`; README del backend actualizado.

**Sin cambios (conservado tal cual):** entidades, relaciones, validaciones (`@NotBlank`, `@Email`, `@Size`…), servicios, repositorios, mappers, auditoría, seeders, `PageResponse`, migraciones Flyway `V1` y `V2`, y las pruebas existentes.

**Frontend:** proyecto nuevo (ver arriba). Las pantallas `LoginPage` y `HomePage` no existían como plantillas: son el mínimo imprescindible para poder autenticarse y comprobar la sesión; `HomePage` es provisional (muestra usuario y roles, y permite cerrar sesión).

## 4. Pruebas y verificación

| Qué | Resultado |
|---|---|
| Frontend: `npm install` | OK (sin vulnerabilidades reportadas) |
| Frontend: `npm run build` (`tsc -b` + `vite build`) | **OK** |
| Frontend: `vite` en modo desarrollo sirve `index.html` | OK |
| Frontend: proxy `/api` hacia el backend | **No verificado**: la prueba contra un servidor simulado devolvió 502 porque el simulado ya había terminado |
| Frontend: flujo de login en navegador | **No verificado** (no hay navegador ni backend en el entorno de trabajo) |
| Backend: compilación y pruebas | **No ejecutadas.** El entorno donde se hizo este trabajo no puede descargar de Maven Central, así que `./mvnw` no puede resolver dependencias. El código del backend **no fue compilado** |
| Backend: prueba nueva `UsuarioSesionDtoTest` | Escrita, **sin ejecutar** |

**Riesgo concreto del backend:** el proyecto usa Spring Boot 4.1 / Spring Security 7, y las clases nuevas de seguridad (`SecurityConfig`, `CsrfCookieFilter`) se escribieron sin poder compilarlas. Antes de seguir, ejecutar en tu máquina `cd backend && ./mvnw clean compile && ./mvnw test -Dtest="*Test"` y revisar el flujo completo de login (ver pendientes). Si algo no compila, lo esperable es un import o una firma de Spring Security 7 en esas dos clases.

## 5. Tareas pendientes

1. **Compilar y probar el backend** (comando arriba) y arrancarlo contra PostgreSQL.
2. **Probar de punta a punta** el login desde el navegador: `GET /api/auth/me` (401 + cookie `XSRF-TOKEN`), `POST /api/auth/login`, `GET /api/auth/me` (200), `POST /api/auth/logout`.
3. **CSRF entre dominios distintos:** si frontend y API se publican en dominios sin relación, el frontend no podrá leer la cookie `XSRF-TOKEN`. La solución recomendada es servir ambos bajo un mismo dominio con proxy inverso; la alternativa sería entregar el token desde un endpoint. No se implementó porque excede esta fase.
4. Pruebas automatizadas: de integración para la seguridad (`@WebMvcTest`/`MockMvc`) y del frontend (Vitest + Testing Library).
5. Recuperar/restablecer contraseña: el backend tiene la entidad `TokenRecuperacion` y la tabla, pero nunca tuvo servicio ni pantallas. No se creó nada; las rutas `/recuperar` y `/restablecer` que antes se permitían en `SecurityConfig` se retiraron porque no existían.
6. Gestión de usuarios, roles y permisos: existen los servicios, pero no había controllers ni pantallas. Pertenece a la Fase 1 (parte visual) y siguientes.
7. Layout general, navegación, modo oscuro y los módulos de los casos de uso del diagrama (catálogo, movimientos de inventario, compras, administración, reportes y consulta): fases posteriores.
8. Despliegue (Docker, CI) y `ENV` de producción del frontend: no solicitado en esta fase.
9. Revisar y cambiar la contraseña `admin` que estuvo en `application.properties` (punto 7 de cambios).
