package cl.donaciones.logistica.service;

import cl.donaciones.logistica.dto.LogisticaDTOs.*;
import cl.donaciones.logistica.model.CentroAcopio;
import cl.donaciones.logistica.model.Envio;
import cl.donaciones.logistica.repository.CentroAcopioRepository;
import cl.donaciones.logistica.repository.EnvioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogisticaServiceImplTest {

    @Mock CentroAcopioRepository centroRepo;
    @Mock EnvioRepository        envioRepo;
    @InjectMocks LogisticaServiceImpl service;

    private CentroAcopio centro;
    private Envio envio;

    @BeforeEach
    void setUp() {
        centro = CentroAcopio.builder()
                .id(1L).nombre("Centro Santiago").direccion("Av. Principal 100")
                .region("Metropolitana").capacidad(500).activo(true).build();

        envio = Envio.builder()
                .id(1L).centroOrigen(centro).destino("Calama")
                .estado(Envio.Estado.PLANIFICADO).build();
    }

    // ── Centros ──────────────────────────────────────────────────────────────

    @Test
    void getAllCentros_returnsAll() {
        when(centroRepo.findAll()).thenReturn(List.of(centro));
        assertThat(service.getAllCentros()).hasSize(1);
        assertThat(service.getAllCentros().get(0).getNombre()).isEqualTo("Centro Santiago");
    }

    @Test
    void getCentrosActivos_returnsOnlyActivos() {
        when(centroRepo.findByActivoTrue()).thenReturn(List.of(centro));
        assertThat(service.getCentrosActivos()).hasSize(1);
    }

    @Test
    void getCentroById_found_returnsResponse() {
        when(centroRepo.findById(1L)).thenReturn(Optional.of(centro));
        CentroAcopioResponse r = service.getCentroById(1L);
        assertThat(r.getId()).isEqualTo(1L);
        assertThat(r.getRegion()).isEqualTo("Metropolitana");
    }

    @Test
    void getCentroById_notFound_throwsException() {
        when(centroRepo.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getCentroById(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("99");
    }

    @Test
    void crearCentro_savesAndReturns() {
        CentroAcopioRequest req = new CentroAcopioRequest();
        req.setNombre("Nuevo Centro"); req.setDireccion("Calle 1");
        req.setRegion("Valparaíso");   req.setCapacidad(200);
        when(centroRepo.save(any())).thenReturn(centro);
        CentroAcopioResponse r = service.crearCentro(req);
        assertThat(r).isNotNull();
        verify(centroRepo).save(any(CentroAcopio.class));
    }

    @Test
    void eliminarCentro_existingId_callsDelete() {
        when(centroRepo.existsById(1L)).thenReturn(true);
        service.eliminarCentro(1L);
        verify(centroRepo).deleteById(1L);
    }

    @Test
    void eliminarCentro_notFound_throwsException() {
        when(centroRepo.existsById(99L)).thenReturn(false);
        assertThatThrownBy(() -> service.eliminarCentro(99L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── Envíos ───────────────────────────────────────────────────────────────

    @Test
    void getAllEnvios_returnsAll() {
        when(envioRepo.findAll()).thenReturn(List.of(envio));
        assertThat(service.getAllEnvios()).hasSize(1);
    }

    @Test
    void getEnvioById_found_returnsResponse() {
        when(envioRepo.findById(1L)).thenReturn(Optional.of(envio));
        EnvioResponse r = service.getEnvioById(1L);
        assertThat(r.getDestino()).isEqualTo("Calama");
    }

    @Test
    void crearEnvio_savesAndReturns() {
        EnvioRequest req = new EnvioRequest();
        req.setCentroOrigenId(1L); req.setDestino("Arica"); req.setEstado("PLANIFICADO");
        when(centroRepo.findById(1L)).thenReturn(Optional.of(centro));
        when(envioRepo.save(any())).thenReturn(envio);
        EnvioResponse r = service.crearEnvio(req);
        assertThat(r).isNotNull();
        verify(envioRepo).save(any(Envio.class));
    }

    @Test
    void actualizarEstadoEnvio_updatesCorrectly() {
        when(envioRepo.findById(1L)).thenReturn(Optional.of(envio));
        when(envioRepo.save(any())).thenReturn(envio);
        EnvioResponse r = service.actualizarEstadoEnvio(1L, "EN_TRANSITO");
        assertThat(r).isNotNull();
        verify(envioRepo).save(any(Envio.class));
    }

    @Test
    void actualizarEstadoEnvio_invalidEstado_throwsException() {
        when(envioRepo.findById(1L)).thenReturn(Optional.of(envio));
        assertThatThrownBy(() -> service.actualizarEstadoEnvio(1L, "INVALIDO"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Estado inválido");
    }

    @Test
    void getEnviosByEstado_returnsFiltered() {
        when(envioRepo.findByEstado(Envio.Estado.PLANIFICADO)).thenReturn(List.of(envio));
        assertThat(service.getEnviosByEstado("PLANIFICADO")).hasSize(1);
    }
}
