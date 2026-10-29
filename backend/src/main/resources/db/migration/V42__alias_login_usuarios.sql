-- Usa los aliases de inicio de sesión como nombre visible en usuarios.
UPDATE usuarios
SET nombre = 'admin', modificado_por = 'sistema', modificado_en = now(), version = version + 1
WHERE lower(email) = lower('admin@motoboutique.local');

UPDATE usuarios
SET nombre = 'almacenero', modificado_por = 'sistema', modificado_en = now(), version = version + 1
WHERE lower(email) = lower('almacenero@motoboutique.local');

UPDATE usuarios
SET nombre = 'contador', modificado_por = 'sistema', modificado_en = now(), version = version + 1
WHERE lower(email) = lower('contador@motoboutique.local');

UPDATE usuarios
SET nombre = 'consulta', modificado_por = 'sistema', modificado_en = now(), version = version + 1
WHERE lower(email) = lower('consulta@motoboutique.local');
