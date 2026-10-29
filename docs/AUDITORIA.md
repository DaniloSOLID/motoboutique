# Auditoría

## Estado backend de catálogo (Prompt 3)

| Elemento | ¿Funciona? | Fuente actual | Endpoint | Tabla BD | Permiso |
|---|---|---|---|---|---|
| Elemento | ¿Funciona? | Fuente actual | Endpoint | Tabla BD | Permiso |
|---|---|---|---|---|---|
| Categorías (búsqueda, filtro, orden, paginación y CRUD) | Sí | API | `/api/catalogo/categorias` | `categorias` | `PRODUCTO_VER` / `PRODUCTO_EDITAR` |
| Marcas (búsqueda, filtro, orden, paginación y CRUD) | Sí | API | `/api/catalogo/marcas` | `marcas` | `PRODUCTO_VER` / `PRODUCTO_EDITAR` |
| Productos (q, categoriaId, marcaId, activo, stockBajo, modeloMotoId, orden y página) | Sí | API | `/api/catalogo/productos` | `productos`, `stock`, `producto_compatibilidades` | `PRODUCTO_VER` |
| Producto (detalle, stock total/por almacén, compatibilidades, imagen y especificaciones) | Sí | API | `/api/catalogo/productos/{id}` | `productos`, `stock`, `almacenes`, `producto_compatibilidades` | `PRODUCTO_VER` |
| Alta, edición y baja lógica de productos | Sí | API | `POST/PUT/DELETE /api/catalogo/productos[/{id}]` | `productos` | `PRODUCTO_EDITAR` |
| Modelos de moto (página y filtros q, marcaId, anio) | Sí | API | `GET /api/catalogo/modelos` | `modelos_moto`, `marcas` | `PRODUCTO_VER` |
| CRUD de modelos de moto | Sí | API | `/api/catalogo/modelos[/{id}]` | `modelos_moto` | `PRODUCTO_EDITAR` |
| Compatibilidad de productos y consulta inversa | Sí | API | `PUT /api/catalogo/productos/{id}/compatibilidades`, `GET /api/catalogo/modelos/{id}/productos` | `producto_compatibilidades` | `PRODUCTO_EDITAR` / `PRODUCTO_VER` |
| Marcas con modelos y años por modelo | Sí | API | `GET /api/catalogo/marcas-con-modelos`, `GET /api/catalogo/modelos/{id}/anios` | `marcas`, `modelos_moto` | `PRODUCTO_VER` |
| Unidades de moto (filtros, detalle y CRUD) | Sí | API | `/api/motos/unidades[/{id}]` | `unidades_moto` | `PRODUCTO_VER` / `PRODUCTO_EDITAR` |
| Transición de estado de unidad | Sí | API | `PATCH /api/motos/unidades/{id}/estado` | `unidades_moto` | `PRODUCTO_EDITAR` |
| Detectar archivo y vista previa sin persistencia | Sí | Archivo enviado por cliente | `POST /api/catalogo/importaciones` | Ninguna | `IMPORTAR_CATALOGO` |
| Validar mapeo, filas válidas y errores por fila/columna | Sí | Archivo enviado por cliente y referencias de BD | `POST /api/catalogo/importaciones/validar` | Solo lectura: `productos`, `categorias`, `marcas` | `IMPORTAR_CATALOGO` |
| Confirmar upsert transaccional e historial/errores | Sí | API | `POST /api/catalogo/importaciones/confirmar` | `productos`, `importaciones`, `importacion_errores` | `IMPORTAR_CATALOGO` |
| Historial y plantilla CSV | Sí | API | `GET /api/catalogo/importaciones`, `GET /api/catalogo/importaciones/plantilla` | `importaciones`, `importacion_errores` | `IMPORTAR_CATALOGO` |

La validación de importación no guarda archivos, filas, errores ni historial; el cliente vuelve a enviar el archivo y el mapeo al confirmar. La confirmación guarda el upsert de productos, el historial y los errores en una sola transacción.

Fecha: 2026-10-28. Las tablas se basan en las rutas y componentes presentes en el repositorio. Estados: **Sí**, **Parcial**, **No** o **Ausente**.

## Encabezado y login comunes

| Pantalla/rol | Elemento | ¿Funciona? | Fuente actual | Endpoint esperado | Tabla de BD | Permiso |
|---|---|---|---|---|---|---|
| Todos | Login por usuario/correo | Parcial | API de sesión | `POST /api/auth/login` | usuarios | Público |
| Todos | Contraseña | Parcial | API de sesión | `POST /api/auth/login` | usuarios | Público |
| Todos | Mostrar contraseña | Sí | Estado local del componente | Ninguno | Ninguna | Público |
| Todos | Recuperar contraseña | No | Nada | `POST /api/auth/recuperar-password` | usuarios, token_recuperacion | Público |
| Todos | Buscador global | No | Nada | `GET /api/busqueda?q=&page=&size=` | Varias | Según resultado |
| Todos | Notificaciones | No | Nada | `GET /api/notificaciones` | notificaciones | NOTIFICACION_VER |
| Todos | Tema claro/oscuro | Sí | `/api/estado/preferencias` | `GET/PUT /api/estado/preferencias` | estado_aplicacion | Sesión autenticada |
| Todos | Navegación lateral | Sí | Configuración frontend | Ninguno | Ninguna | Según ruta |
| Todos | Perfil y cerrar sesión | Parcial | API al salir | `POST /api/auth/logout` | usuarios, sesión HTTP | Sesión autenticada |

## Administrador

### Dashboard

| Elemento | ¿Funciona? | Fuente actual | Endpoint esperado | Tabla de BD | Permiso |
|---|---|---|---|---|---|
| Tarjetas de métricas | Sí | API/BD | `GET /api/dashboard/admin` | usuarios, productos, stock, movimientos_inventario, ajustes_inventario | ADMIN |
| Gráfica y tabla de actividad | Sí | API/BD | `GET /api/dashboard/admin` | auditoria_eventos | ADMIN |

### Usuarios

| Elemento | ¿Funciona? | Fuente actual | Endpoint esperado | Tabla de BD | Permiso |
|---|---|---|---|---|---|
| Buscador | Sí | API | `GET /api/usuarios?q=&page=&size=` | usuarios | USUARIO_VER |
| Filtro por rol | Sí | API | `GET /api/usuarios?rol=` | usuario_roles, roles | USUARIO_VER |
| Nuevo usuario | Sí | API | `POST /api/usuarios` | usuarios, usuario_roles | USUARIO_CREAR |
| Nombre | Sí | API | POST/PUT usuario | usuarios | USUARIO_CREAR/EDITAR |
| Correo | Sí | API | POST/PUT usuario | usuarios | USUARIO_CREAR/EDITAR |
| Alias de login | Parcial | API | POST/PUT usuario | usuarios.alias_login | USUARIO_CREAR/EDITAR |
| Rol | Sí | API | POST/PUT usuario | usuario_roles, roles | USUARIO_EDITAR |
| Botón estado | Sí | API | `PATCH /api/usuarios/{id}/estado` | usuarios.activo | USUARIO_EDITAR |
| Editar | Sí | API | `PUT /api/usuarios/{id}` | usuarios | USUARIO_EDITAR |
| Eliminar | Sí | API lógico | `DELETE /api/usuarios/{id}` | usuarios.eliminado | USUARIO_ELIMINAR |
| Tabla | Sí | API | `GET /api/usuarios` | usuarios, usuario_roles, roles | USUARIO_VER |

### Roles y permisos

| Elemento | ¿Funciona? | Fuente actual | Endpoint esperado | Tabla de BD | Permiso |
|---|---|---|---|---|---|
| Tabla de módulos | Parcial | Dato fijo | `GET /api/permisos` | permisos, rol_permisos | ROL_VER |
| Buscador de roles | No | Nada | `GET /api/roles?q=&page=&size=` | roles | ROL_VER |
| Casillas de permisos | No | Estado fijo no persistido | `PUT /api/roles/{id}/permisos` | rol_permisos | PERMISO_EDITAR |

### Configuración

| Elemento | ¿Funciona? | Fuente actual | Endpoint esperado | Tabla de BD | Permiso |
|---|---|---|---|---|---|
| Preferencias | Parcial | localStorage en tema | `GET/PUT /api/estado/configuracion` | estado_aplicacion | CONFIGURACION_EDITAR |
| Parámetros | No | Nada | `GET /api/configuracion` | configuracion | CONFIGURACION_VER |
| Guardar parámetros | No | Nada | `PUT /api/configuracion/{clave}` | configuracion | CONFIGURACION_EDITAR |

### Aprobaciones

| Elemento | ¿Funciona? | Fuente actual | Endpoint esperado | Tabla de BD | Permiso |
|---|---|---|---|---|---|
| Tabla | Parcial | Filas visuales | `GET /api/aprobaciones?page=&size=&estado=` | aprobaciones | APROBACION_VER |
| Buscador | No | Nada | `GET /api/aprobaciones?q=` | aprobaciones | APROBACION_VER |
| Filtro estado | No | Nada | `GET /api/aprobaciones?estado=` | aprobaciones | APROBACION_VER |
| Aprobar | No | Sin persistencia | `POST /api/aprobaciones/{id}/aprobar` | aprobaciones, audit_log | APROBACION_EDITAR |
| Rechazar | No | Sin persistencia | `POST /api/aprobaciones/{id}/rechazar` | aprobaciones, audit_log | APROBACION_EDITAR |

## Almacenero

### Dashboard y movimientos

| Pantalla | Elemento | ¿Funciona? | Fuente actual | Endpoint esperado | Tabla de BD | Permiso |
|---|---|---|---|---|---|---|
| Dashboard | Métricas | Parcial | API resumen/inventario | `GET /api/inventario/resumen` | productos, movimientos | INVENTARIO_VER |
| Dashboard | Stock bajo | Parcial | API productos | `GET /api/inventario/productos?stockBajo=true` | productos, stocks | INVENTARIO_VER |
| Dashboard | Gráfica | No | Nada/fijo | `GET /api/reportes/movimientos?periodo=` | movimientos_inventario | REPORTE_VER |
| Movimientos | Tabla | Parcial | API movimientos | `GET /api/inventario/movimientos` | movimientos_inventario | MOVIMIENTO_VER |
| Movimientos | Buscador/filtro | No | Nada | `GET /api/inventario/movimientos?q=&tipo=&desde=&hasta=` | movimientos_inventario | MOVIMIENTO_VER |
| Entradas | Formulario | No | Opciones de demostración y sin submit | `POST /api/inventario/movimientos/entrada` | movimientos, kardex, stocks | MOVIMIENTO_CREAR |
| Salidas | Formulario | No | Opciones de demostración y sin submit | `POST /api/inventario/movimientos/salida` | movimientos, kardex, stocks | MOVIMIENTO_CREAR |
| Transferencias | Formulario | No | Opciones de demostración y sin submit | `POST /api/inventario/movimientos/transferencia` | movimientos, kardex, stocks | MOVIMIENTO_CREAR |
| Todas | Confirmar movimiento | No | Botón visual | POST específico por tipo | movimientos, kardex | MOVIMIENTO_CREAR |

### Conteo físico y ajustes

| Elemento | ¿Funciona? | Fuente actual | Endpoint esperado | Tabla de BD | Permiso |
|---|---|---|---|---|---|
| Tabla conteo | No | Dato fijo/vacío | `GET /api/inventario/conteos` | conteos_fisicos, detalle_conteo | INVENTARIO_VER |
| Iniciar conteo | No | Nada | `POST /api/inventario/conteos` | conteos_fisicos | INVENTARIO_EDITAR |
| Cantidad contada | No | Nada | `PUT /api/inventario/conteos/{id}/detalle/{productoId}` | detalle_conteo | INVENTARIO_EDITAR |
| Cerrar conteo | No | Nada | `POST /api/inventario/conteos/{id}/cerrar` | conteos, ajustes | INVENTARIO_EDITAR |
| Ajustes | Tabla | No | Dato fijo/vacío | `GET /api/inventario/ajustes` | ajustes_inventario | INVENTARIO_VER |
| Ajustes | Crear | No | Formulario visual | `POST /api/inventario/ajustes` | ajustes, movimientos | INVENTARIO_EDITAR |

### Productos, catálogo, compras

| Pantalla | Elemento | ¿Funciona? | Fuente actual | Endpoint esperado | Tabla de BD | Permiso |
|---|---|---|---|---|---|---|
| Productos | Tabla | Parcial | API catálogo/inventario | `GET /api/catalogo/productos` | productos | PRODUCTO_VER |
| Productos | Buscador/filtros | No | Controles visuales | `GET /api/catalogo/productos?q=&categoria=&marca=&page=&size=` | productos, categorias, marcas | PRODUCTO_VER |
| Productos | Nuevo/editar/eliminar | No/Parcial | Modal o acción visual | POST/PUT/DELETE `/api/catalogo/productos/{id}` | productos.eliminado | PRODUCTO_EDITAR |
| Importación | Wizard | No | UI visual | `POST /api/catalogo/importaciones` | productos, importaciones | PRODUCTO_EDITAR |
| Motocicletas | Listar/buscar | No | API REST no consumida por pantalla | `GET /api/motos?q=&page=&size=` | motos, modelos_moto | PRODUCTO_VER |
| Motocicletas | Crear/editar/eliminar | No | Nada | CRUD `/api/motos/{id}` | motos | PRODUCTO_EDITAR |
| Proveedores | Tabla | Parcial | API compras | `GET /api/compras/proveedores` | proveedores | COMPRA_VER |
| Proveedores | Crear/editar | No | Nada | POST/PUT `/api/compras/proveedores/{id}` | proveedores | COMPRA_EDITAR |
| Órdenes | Tabla | Parcial | API compras | `GET /api/compras/ordenes` | ordenes_compra, detalle_orden_compra | COMPRA_VER |
| Órdenes | Crear/cancelar | No | Botones visuales | POST/PUT `/api/compras/ordenes/{id}` | ordenes_compra, detalles | COMPRA_EDITAR |
| Recepciones | Tabla | Parcial | API compras | `GET /api/compras/recepciones` | recepciones_compra | COMPRA_VER |
| Recepciones | Registrar | No | Formulario visual | `POST /api/compras/recepciones` | recepciones, movimientos | COMPRA_EDITAR |

## Contador

| Pantalla | Elemento | ¿Funciona? | Fuente actual | Endpoint esperado | Tabla de BD | Permiso |
|---|---|---|---|---|---|---|
| Valorización | Tabla | No | Nada | `GET /api/reportes/valorizacion?fecha=` | movimientos, productos, costos_promedio | REPORTE_VER |
| Valorización | Buscador/filtros | No | Nada | Query `q`, producto, fecha | productos, costos_promedio | REPORTE_VER |
| Valorización | Exportar | No | Nada | `POST /api/reportes/exportar/valorizacion` | Reporte generado | REPORTE_EXPORTAR |
| Kardex | Tabla | No | Componente muerto eliminado | `GET /api/reportes/kardex?q=&desde=&hasta=` | kardex, productos | REPORTE_VER |
| Kardex | Gráfica | No | Nada | `GET /api/reportes/kardex/resumen` | kardex | REPORTE_VER |
| Kardex | Exportar | No | Nada | `POST /api/reportes/exportar/kardex` | Reporte generado | REPORTE_EXPORTAR |
| Reportes | Lista/gráfica/filtros | No | Nada; componente muerto eliminado | `GET /api/reportes/financieros` | ventas, compras, movimientos | REPORTE_VER |

## Consulta

| Pantalla | Elemento | ¿Funciona? | Fuente actual | Endpoint esperado | Tabla de BD | Permiso |
|---|---|---|---|---|---|---|
| Stock | Tabla | Parcial | API inventario | `GET /api/inventario/productos?q=&page=&size=` | productos, stocks, almacenes | INVENTARIO_VER |
| Stock | Buscador/filtro | No | Nada | `GET /api/inventario/productos?q=&almacenId=&categoriaId=` | productos, stocks, almacenes, categorias | INVENTARIO_VER |
| Compatibilidades | Buscador/modelo | No | Componente fijo eliminado | `GET /api/motos/{modeloId}/compatibles?q=&page=&size=` | modelo_producto, productos | PRODUCTO_VER |
| Compatibilidades | Tabla compatibles | No | Nada | Endpoint anterior | modelo_producto, productos | PRODUCTO_VER |
| Catálogo | Buscador | No | Componente fijo eliminado | `GET /api/catalogo/productos?q=` | productos | PRODUCTO_VER |
| Catálogo | Tarjetas/ficha técnica | No | Nada | `GET /api/catalogo/productos/{id}` | productos, especificaciones_producto | PRODUCTO_VER |

## Brechas confirmadas del backend

### Endpoints ausentes o incompletos

- Contrato y recuperación de sesión/login: falta recuperar contraseña y validar por completo el flujo; revisar `AuthController` y `UsuarioDetailsService` antes de añadir rutas.
- Roles/permisos: `GET/PUT /api/roles`, `/api/permisos`, `/api/roles/{id}/permisos`.
- Dashboard y búsqueda global/notificaciones.
- Movimientos de entrada, salida, transferencia, conteo físico, ajustes y recepción.
- Kardex, valorización, reportes financieros y exportaciones.
- Aprobaciones, configuración avanzada y auditoría.
- CRUD completo de proveedores, órdenes y recepciones.
- Estado de aplicación y tema deben persistirse por API; el frontend todavía usa `localStorage`.

### Entidades y servicios

`MovimientoInventarioService`, `Notificacion`, `DashboardService`, `ReporteService`, `AprobacionService`, conteo físico, ajuste, Kardex, costo promedio, órdenes y recepciones. Falta completar repositorios y servicios de compras.

## Brechas confirmadas de la base de datos

Se comparan solo las necesidades visibles; cualquier migración futura será nueva y posterior a V42.

| Necesidad UI | Tabla/columna que falta o falta confirmar |
|---|---|
| Recuperar contraseña | relación completa de `token_recuperacion` con `usuarios`, expiración y uso |
| Roles/permisos dinámicos | `roles`, `permisos`, `usuario_roles`, `rol_permisos` |
| Dashboard/gráficas | movimiento por fecha, ventas/compras y vistas de series agregadas |
| Notificaciones | `notificaciones`: usuario, tipo, entidad, leída, fecha |
| Tema/estado | columna o registro de `estado_aplicacion` por usuario/módulo |
| Movimientos completos | almacén origen/destino, Kardex y stock; relación con producto |
| Conteo físico | `conteos_fisicos`, `detalle_conteo`, cantidades esperadas y contadas |
| Ajustes | `ajustes_inventario`: motivo, producto, cantidad, almacén, usuario |
| Kardex/valorización | costo unitario, costo total y costo promedio ponderado por producto/almacén |
| Compras | detalles, estados, recepciones y relaciones de proveedores/órdenes |
| Motocicletas/compatibilidad | `modelos_moto`, `unidades_moto`, `producto_compatibilidades` existen; no se requiere una tabla adicional para el alcance auditado |
| Aprobaciones | entidad aprobable, estado, motivo, aprobador y fechas |
| Reportes | periodo, moneda, usuario generador y datos exportables |

## Validación requerida

Se debe ejecutar backend con `./mvnw test` y frontend con `npm run build`, además de lint/tests disponibles. La ejecución con la configuración actual también debe corregirse si las credenciales de Supabase son incorrectas o si el contexto de pruebas intenta conectar a Supabase.

## Endpoints agregados de dashboard, finanzas y reportes

Actualización: 2026-10-28. **Disponible** indica que la ruta está implementada en backend; **API/BD** indica que los valores provienen de consultas a PostgreSQL, no de datos de ejemplo.

| Rol/pantalla | Elemento | ¿Funciona? | Fuente actual | Endpoint | Tablas | Permiso |
|---|---|---|---|---|---|---|
| Administrador / Dashboard | Usuarios activos, productos activos, stock valorizado, alertas y ajustes pendientes | Disponible | API/BD | `GET /api/dashboard/admin` | `usuarios`, `productos`, `stock`, `movimientos_inventario`, `ajustes_inventario` | `ADMIN` |
| Administrador / Dashboard | Actividad reciente | Disponible | API/BD | `GET /api/dashboard/admin` | `auditoria_eventos` | `ADMIN` |
| Almacenero / Dashboard | Entradas y salidas del día, transferencias en tránsito, stock bajo | Disponible | API/BD | `GET /api/dashboard/almacen?dias=7|30|90` | `movimientos_inventario`, `transferencias`, `stock`, `productos` | `INVENTARIO_VER` |
| Almacenero / Dashboard | Serie diaria con ceros para días sin movimientos y lista de críticos | Disponible | API/BD | `GET /api/dashboard/almacen?dias=7|30|90` | `movimientos_inventario`, `stock`, `productos`, `categorias` | `INVENTARIO_VER` |
| Finanzas / Dashboard | Valor de inventario, compras del mes, rotación de salidas a costo promedio y serie mensual de compras | Disponible | API/BD | `GET /api/dashboard/finanzas` | `stock`, `movimientos_inventario`, `ordenes_compra` | `REPORTE_VER` |
| Consulta / Dashboard | Productos disponibles y agotados | Disponible | API/BD | `GET /api/dashboard/consulta` | `productos`, `stock` | `PRODUCTO_VER` |
| Finanzas / Valorización | Saldo y costo promedio al corte, filtro, totales y agrupación por categoría | Disponible | API/BD; reconstrucción del último movimiento hasta el corte | `GET /api/finanzas/valorizacion?fechaCorte=&almacenId=&categoriaId=&q=&page=&size=` | `movimientos_inventario`, `productos`, `categorias` | `REPORTE_VER` |
| Reportes | Valorización | Disponible | API/BD | `GET /api/reportes/valorizacion` | `movimientos_inventario`, `productos`, `categorias` | `REPORTE_VER`; exportar: `REPORTE_EXPORTAR` |
| Reportes | Kardex (requiere `productoId` y `almacenId`) | Disponible | API/BD | `GET /api/reportes/kardex?productoId=&almacenId=&desde=&hasta=` | `movimientos_inventario`, `productos`, `almacenes` | `REPORTE_VER`; exportar: `REPORTE_EXPORTAR` |
| Reportes | Stock bajo (incluye agotados) | Disponible | API/BD | `GET /api/reportes/stock-bajo?almacenId=&categoriaId=&q=&page=&size=` | `stock`, `productos`, `categorias`, `almacenes` | `REPORTE_VER`; exportar: `REPORTE_EXPORTAR` |
| Reportes | Movimientos por periodo | Disponible | API/BD | `GET /api/reportes/movimientos-por-periodo?desde=&hasta=&almacenId=&productoId=&q=&page=&size=` | `movimientos_inventario`, `productos`, `almacenes` | `REPORTE_VER`; exportar: `REPORTE_EXPORTAR` |
| Reportes | Compras por proveedor | Disponible | API/BD | `GET /api/reportes/compras-por-proveedor?desde=&hasta=&q=&page=&size=` | `ordenes_compra`, `proveedores` | `REPORTE_VER`; exportar: `REPORTE_EXPORTAR` |
| Reportes | Valorización y cantidad por categoría | Disponible | API/BD | `GET /api/reportes/por-categoria?fechaCorte=&almacenId=&categoriaId=&q=&page=&size=` | `movimientos_inventario`, `productos`, `categorias` | `REPORTE_VER`; exportar: `REPORTE_EXPORTAR` |
| Reportes | Exportación CSV, XLSX o PDF con datos de empresa y fecha | Disponible | API/BD | `GET /api/reportes/{tipo}?formato=csv|xlsx|pdf` | `configuracion_empresa` y tablas del reporte | `REPORTE_VER` + `REPORTE_EXPORTAR` |

La valorización usa cantidad por costo promedio ponderado del movimiento, no precio de venta. Los endpoints de exportación serializan la página seleccionada y respetan los filtros enviados.

## Reestructuración de frontend

Las rutas de navegación se definen en React Router y se filtran por permisos del usuario. La tabla de usuarios ya usa filtros y paginación en URL con consulta a servidor; las pantallas no migradas muestran estado vacío sin datos de ejemplo. Tema, tamaño de página y filtros de URL se persisten mediante `/api/estado/preferencias`. El buscador global y las notificaciones que no tenían endpoint funcional se retiraron del encabezado.
