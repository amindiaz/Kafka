package cl.donaciones.necesidades.service;

import cl.donaciones.necesidades.dto.NecesidadDTOs.*;
import cl.donaciones.necesidades.model.Necesidad;
import cl.donaciones.necesidades.repository.NecesidadRepository;
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
class NecesidadServiceImplTest {

    @Mock  NecesidadRepository necesidadRepository;
    @InjectMocks NecesidadServiceImpl service;

    private Necesidad necesidad;

    @BeforeEach
    void setUp() {
        necesidad = Necesidad.builder()
                .id(1L)
                .recurso("Agua potable")
                .cantidad(100)
                .unidad("litros")
                .ubicacion("Calama, Antofagasta")
                .prioridad(Necesidad.Prioridad.ALTA)
                .estado(Necesidad.Estado.PENDIENTE)
                .build();
    }

    @Test
    void getAll_returnsAllNecesidades() {
        when(necesidadRepository.findAll()).thenReturn(List.of(necesidad));
        List<NecesidadResponse> result = service.getAll();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRecurso()).isEqualTo("Agua potable");
    }

    @Test
    void getById_found_returnsResponse() {
        when(necesidadRepository.findById(1L)).thenReturn(Optional.of(necesidad));
        NecesidadResponse r = service.getById(1L);
        assertThat(r.getId()).isEqualTo(1L);
        assertThat(r.getUbicacion()).isEqualTo("Calama, Antofagasta");
    }

    @Test
    void getById_notFound_throwsException() {
        when(necesidadRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getById(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("99");
    }

    @Test
    void crear_savesAndReturnsResponse() {
        NecesidadRequest req = new NecesidadRequest();
        req.setRecurso("Alimentos");
        req.setCantidad(50);
        req.setUbicacion("Valparaíso");
        req.setPrioridad("MEDIA");

        when(necesidadRepository.save(any())).thenReturn(necesidad);

        NecesidadResponse r = service.crear(req);
        assertThat(r).isNotNull();
        verify(necesidadRepository).save(any(Necesidad.class));
    }

    @Test
    void actualizarEstado_updatesCorrectly() {
        when(necesidadRepository.findById(1L)).thenReturn(Optional.of(necesidad));
        when(necesidadRepository.save(any())).thenReturn(necesidad);

        NecesidadResponse r = service.actualizarEstado(1L, "EN_ATENCION");
        assertThat(r).isNotNull();
        verify(necesidadRepository).save(any(Necesidad.class));
    }

    @Test
    void actualizarEstado_invalidEstado_throwsException() {
        when(necesidadRepository.findById(1L)).thenReturn(Optional.of(necesidad));
        assertThatThrownBy(() -> service.actualizarEstado(1L, "INVALIDO"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Estado inválido");
    }

    @Test
    void eliminar_existingId_callsDelete() {
        when(necesidadRepository.existsById(1L)).thenReturn(true);
        service.eliminar(1L);
        verify(necesidadRepository).deleteById(1L);
    }

    @Test
    void eliminar_notFound_throwsException() {
        when(necesidadRepository.existsById(99L)).thenReturn(false);
        assertThatThrownBy(() -> service.eliminar(99L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getByEstado_returnsFiltered() {
        when(necesidadRepository.findByEstado(Necesidad.Estado.PENDIENTE)).thenReturn(List.of(necesidad));
        List<NecesidadResponse> result = service.getByEstado("PENDIENTE");
        assertThat(result).hasSize(1);
    }

    @Test
    void getByPrioridad_returnsFiltered() {
        when(necesidadRepository.findByPrioridad(Necesidad.Prioridad.ALTA)).thenReturn(List.of(necesidad));
        List<NecesidadResponse> result = service.getByPrioridad("ALTA");
        assertThat(result).hasSize(1);
    }
}
