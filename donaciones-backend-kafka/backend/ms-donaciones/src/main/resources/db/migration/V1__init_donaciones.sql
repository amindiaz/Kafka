CREATE TABLE IF NOT EXISTS donaciones (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    tipo         VARCHAR(100) NOT NULL,
    cantidad     INT          NOT NULL,
    unidad       VARCHAR(50)  NOT NULL DEFAULT 'unidades',
    origen       VARCHAR(200) NOT NULL,
    centro_acopio VARCHAR(200) NOT NULL,
    estado       VARCHAR(30)  NOT NULL DEFAULT 'Recibida',
    fecha        DATE,
    usuario_id   BIGINT,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
