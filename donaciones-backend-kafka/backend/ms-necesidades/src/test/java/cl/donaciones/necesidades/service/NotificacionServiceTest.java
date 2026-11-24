package cl.donaciones.necesidades.service;

import cl.donaciones.necesidades.messaging.dto.DonacionEvent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificacionServiceTest {

    private final NotificacionService service = new NotificacionService();

    @Test
    void notificarNuevaDonacion_eventoValido_noFalla() {
        DonacionEvent e = new DonacionEvent(null, "DONACION_CREADA", null, 1L, "Ropa", 5, "u", "o", "c",
                "Recibida", null, 1L, null);
        assertThatCode(() -> service.notificarNuevaDonacion(e)).doesNotThrowAnyException();
    }

    @Test
    void notificarNuevaDonacion_sinId_lanzaIllegalArgument_para_ir_a_dlq() {
        DonacionEvent e = new DonacionEvent(null, null, null, null, null, null, null, null, null, null, null, null, null);
        assertThatThrownBy(() -> service.notificarNuevaDonacion(e)).isInstanceOf(IllegalArgumentException.class);
    }
}
