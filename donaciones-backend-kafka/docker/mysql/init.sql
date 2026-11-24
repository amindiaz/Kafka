-- ╔══════════════════════════════════════════════════════════════════════╗
-- ║  Script de inicialización MySQL para Donaciones                     ║
-- ║  Se ejecuta UNA sola vez al crear el contenedor.                    ║
-- ║  Flyway de cada microservicio crea las tablas después.              ║
-- ╚══════════════════════════════════════════════════════════════════════╝

CREATE DATABASE IF NOT EXISTS usuarios_db   CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS donaciones_db  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS necesidades_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS logistica_db   CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Dar permisos al usuario de aplicación en todas las bases de datos
GRANT ALL PRIVILEGES ON usuarios_db.*    TO 'donaciones_user'@'%';
GRANT ALL PRIVILEGES ON donaciones_db.*  TO 'donaciones_user'@'%';
GRANT ALL PRIVILEGES ON necesidades_db.* TO 'donaciones_user'@'%';
GRANT ALL PRIVILEGES ON logistica_db.*   TO 'donaciones_user'@'%';

FLUSH PRIVILEGES;
