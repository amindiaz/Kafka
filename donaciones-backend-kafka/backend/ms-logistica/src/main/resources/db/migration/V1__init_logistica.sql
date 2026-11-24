CREATE TABLE IF NOT EXISTS centros_acopio (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    nombre      VARCHAR(200) NOT NULL,
    direccion   VARCHAR(300) NOT NULL,
    region      VARCHAR(100) NOT NULL,
    capacidad   INT          NOT NULL DEFAULT 0,
    responsable VARCHAR(150),
    telefono    VARCHAR(20),
    activo      TINYINT(1)   NOT NULL DEFAULT 1,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS envios (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    centro_origen_id   BIGINT       NOT NULL,
    destino            VARCHAR(300) NOT NULL,
    descripcion        TEXT,
    estado             VARCHAR(30)  NOT NULL DEFAULT 'PLANIFICADO',
    responsable        VARCHAR(150),
    fecha_salida       DATE,
    fecha_entrega_est  DATE,
    fecha_entrega_real DATE,
    donacion_id        BIGINT,
    created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_envio_centro FOREIGN KEY (centro_origen_id) REFERENCES centros_acopio(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
