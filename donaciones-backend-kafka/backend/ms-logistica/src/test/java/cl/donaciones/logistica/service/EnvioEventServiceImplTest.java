package cl.donaciones.logistica.service;

import cl.donaciones.logistica.messaging.dto.DonacionEvent;
import cl.donaciones.logistica.model.CentroAcopio;
import cl.donaciones.logistica.model.Envio;
import cl.donaciones.logistica.repository.CentroAcopioRepository;
import cl.donaciones.logistica.repository.EnvioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnvioEventServiceImplTest {

    @Mock CentroAcopioRepository centroRepo;
    @Mock EnvioRepository envioRepo;
    @InjectMocks EnvioEventServiceImpl service;

    CentroAcopio centro;

    @BeforeEach
    void setUp() {
        centro = CentroAcopio.builder().id(1L).nombre("Santiago Norte").direccion("x")
                .region("RM").capacidad(10).activo(true).build();
    }

    private DonacionEvent evento(String estado, String centroNombre) {
        return new DonacionEvent(null, "X", null, 5L, "Ropa", 10, "prendas", "Particular",
                centroNombre, estado, null, 1L, null);
    }

    @Test
    void crearEnvio_creaEnvioPlanificado() {
        when(envioRepo.findByDonacionId(5L)).thenReturn(List.of());
        when(centroRepo.findFirstByNombreIgnoreCase("Santiago Norte")).thenReturn(Optional.of(centro));
        when(envioRepo.save(any(Envio.class))).thenAnswer(i -> i.getArgument(0));

        service.crearEnvioDesdeDonacion(evento("Recibida", "Santiago Norte"));

        ArgumentCaptor<Envio> captor = ArgumentCaptor.forClass(Envio.class);
        verify(envioRepo).save(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo(Envio.Estado.PLANIFICADO);
        assertThat(captor.getValue().getDonacionId()).isEqualTo(5L);
        assertThat(captor.getValue().getCentroOrigen()).isSameAs(centro);
    }

    @Test
    void crearEnvio_esIdempotente() {
        when(envioRepo.findByDonacionId(5L)).thenReturn(List.of(new Envio()));

        service.crearEnvioDesdeDonacion(evento("Recibida", "Santiago Norte"));

        verify(envioRepo, never()).save(any());
    }

    @Test
    void crearEnvio_centroInexistente_lanzaIllegalArgument_para_ir_a_dlq() {
        when(envioRepo.findByDonacionId(5L)).thenReturn(List.of());
        when(centroRepo.findFirstByNombreIgnoreCase("Otro")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crearEnvioDesdeDonacion(evento("Recibida", "Otro")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void crearEnvio_eventoInvalido_lanzaIllegalArgument() {
        assertThatThrownBy(() -> service.crearEnvioDesdeDonacion(
                new DonacionEvent(null, null, null, null, null, null, null, null, null, null, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sincronizarEstado_distribuida_marcaEntregado() {
        Envio e = Envio.builder().id(1L).estado(Envio.Estado.EN_TRANSITO).build();
        when(envioRepo.findByDonacionId(5L)).thenReturn(List.of(e));

        service.sincronizarEstadoEnvio(evento("Distribuida", "Santiago Norte"));

        assertThat(e.getEstado()).isEqualTo(Envio.Estado.ENTREGADO);
        assertThat(e.getFechaEntregaReal()).isNotNull();
        verify(envioRepo).saveAll(List.of(e));
    }

    @Test
    void sincronizarEstado_recibida_noHaceNada() {
        service.sincronizarEstadoEnvio(evento("Recibida", "Santiago Norte"));

        verifyNoInteractions(envioRepo);
    }
}
