package cl.donaciones.donaciones.messaging.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Nombres de exchange y routing keys de ms-donaciones (productor).
 * Todos los valores viven en application.yml bajo {@code app.messaging}.
 * No debe haber nombres escritos directamente en el código de negocio.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.messaging")
public class MessagingProperties {

    /** Exchange topic donde se publican los eventos de donaciones. */
    private String exchange;

    private RoutingKeys routingKeys = new RoutingKeys();

    @Getter
    @Setter
    public static class RoutingKeys {
        private String donacionCreada;
        private String donacionEstado;
    }
}
