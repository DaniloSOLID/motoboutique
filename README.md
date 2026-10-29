# Moto Boutique

Sistema interno de **control de inventario** para una tienda de motos y accesorios (Perú). El proyecto está separado en dos aplicaciones independientes:

| Carpeta | Qué es | Tecnología |
|---|---|---|
| [`backend/`](backend) | API REST | Java 21, Spring Boot 4.1, Spring Security, Spring Data JPA, Flyway, Maven, PostgreSQL |
| [`frontend/`](frontend) | Aplicación web (SPA) | React, TypeScript, Vite, Tailwind CSS |

Para el detalle de la arquitectura, los cambios, las pruebas y lo pendiente, lee [`PROGRESS.md`](PROGRESS.md).

## Requisitos

- JDK 21
- PostgreSQL 16 o superior
- Node.js 20.19 o superior (o 22.12+) y npm
- Git (en Windows puedes usar PowerShell, CMD o Git Bash)

## Ejecutar en local

Necesitas dos terminales: una para el backend y otra para el frontend.

### 1. Backend (API en el puerto 8081)

1. Crea en PostgreSQL una base de datos **vacía** llamada `motoboutique` (minúsculas, codificación UTF8).
2. Entra a `backend` y crea el archivo de entorno:

   ```bash
   cd backend
   cp .env.example .env
   ```

   En PowerShell, usa `Copy-Item .env.example .env` en lugar de `cp`.

3. Edita `.env` y completa al menos `DB_PASSWORD`, `ADMIN_PASSWORD` (8 caracteres o más, sin `\`) y, si quieres usuarios de prueba, `DEMO_PASSWORD`.
4. Arranca:

   ```bash
   # Git Bash
   ./mvnw spring-boot:run
   ```

   En PowerShell o CMD, también desde `backend`, ejecuta `.\mvnw.cmd spring-boot:run`.

   Flyway crea las tablas y el `DataSeeder` carga permisos, roles base y usuarios. Se puede repetir sin duplicar datos.

### 2. Frontend (puerto 5173)

```bash
cd frontend
cp .env.example .env     # opcional: los valores por defecto sirven en desarrollo
npm install
npm run dev
```

Abre <http://localhost:5173> e inicia sesión con `ADMIN_EMAIL` y `ADMIN_PASSWORD` del `.env` del backend. En desarrollo, Vite reenvía las llamadas `/api` al backend (`VITE_DEV_PROXY_TARGET`, por defecto `http://localhost:8081`), de modo que el navegador ve un solo origen y no hace falta configurar CORS.

## Compilar

```bash
# Backend: JAR ejecutable en backend/target
cd backend && ./mvnw clean package

# Frontend: archivos estáticos en frontend/dist
cd frontend && npm run build
```

## Pruebas

```bash
cd backend && ./mvnw test -Dtest="*Test"   # pruebas unitarias, sin base de datos
```

El frontend todavía no tiene pruebas automatizadas (ver `PROGRESS.md`); `npm run build` ejecuta la verificación de tipos de TypeScript.

## Variables de entorno

**Backend** (`backend/.env`, ver `backend/.env.example`): `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `DEMO_PASSWORD`, `CORS_ALLOWED_ORIGINS` y, solo en producción, `SESSION_COOKIE_SAME_SITE`.

**Frontend** (`frontend/.env`, ver `frontend/.env.example`): `VITE_API_BASE_URL` (vacío en desarrollo) y `VITE_DEV_PROXY_TARGET`.

Los archivos `.env` no se suben a Git. Los `.env.example` no contienen credenciales reales.

## Publicar frontend y backend

Lo más simple es servir ambos bajo el mismo dominio (por ejemplo un proxy inverso que reenvíe `/api` al backend). Si los publicas en dominios distintos, define `VITE_API_BASE_URL` al compilar el frontend, agrega su origen a `CORS_ALLOWED_ORIGINS` y usa `SESSION_COOKIE_SAME_SITE=none` (requiere HTTPS). Ten en cuenta que, entre dominios sin relación, el navegador no deja que el frontend lea la cookie del token CSRF; ver "Tareas pendientes" en `PROGRESS.md`.
