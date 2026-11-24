CREATE TABLE IF NOT EXISTS necesidades (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    recurso     VARCHAR(150)  NOT NULL,
    cantidad    INT           NOT NULL,
    unidad      VARCHAR(50)   NOT NULL DEFAULT 'unidades',
    descripcion TEXT,
    ubicacion   VARCHAR(300)  NOT NULL,
    latitud     DECIMAL(10,7),
    longitud    DECIMAL(10,7),
    prioridad   VARCHAR(20)   NOT NULL DEFAULT 'MEDIA',
    estado      VARCHAR(30)   NOT NULL DEFAULT 'PENDIENTE',
    reportado_por VARCHAR(150),
    usuario_id  BIGINT,
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
