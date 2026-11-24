package cl.donaciones.donaciones.service;

import cl.donaciones.donaciones.dto.DonacionDTOs.DonacionRequest;
import cl.donaciones.donaciones.dto.DonacionDTOs.DonacionResponse;
import cl.donaciones.donaciones.messaging.producer.DonacionEventPublisher;
import cl.donaciones.donaciones.model.Donacion;
import cl.donaciones.donaciones.repository.DonacionRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para DonacionServiceImpl.
 * Patrón AAA: Arrange → Act → Assert
 * Cobertura objetivo: ≥ 60 % de la capa service.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DonacionServiceImpl - Tests Unitarios")
class DonacionServiceImplTest {

    // ── Mocks ─────────────────────────────────────────────────────────────────
    @Mock
    private DonacionRepository donacionRepository;

    @Mock
    private DonacionEventPublisher eventPublisher;

    @InjectMocks
    private DonacionServiceImpl donacionService;

    // ── Datos de prueba ───────────────────────────────────────────────────────
    private Donacion donacion1;
    private Donacion donacion2;

    @BeforeEach
    void setUp() {
        donacion1 = Donacion.builder()
                .id(1L)
                .tipo("Ropa")
                .cantidad(50)
                .unidad("prendas")
                .origen("Particular")
                .centroAcopio("Santiago Norte")
                .estado(Donacion.Estado.Recibida)
                .fecha(LocalDate.of(2025, 5, 1))
                .usuarioId(3L)
                .build();

        donacion2 = Donacion.builder()
                .id(2L)
                .tipo("Alimentos")
                .cantidad(200)
                .unidad("kg")
                .origen("Empresa")
                .centroAcopio("Valparaíso")
                .estado(Donacion.Estado.En_transito)
                .fecha(LocalDate.of(2025, 5, 2))
                .usuarioId(3L)
                .build();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TEST 1: getAll()
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("getAll() debe retornar lista de todas las donaciones")
    void getAll_debeRetornarTodasLasDonaciones() {
        // Arrange
        when(donacionRepository.findAll()).thenReturn(Arrays.asList(donacion1, donacion2));

        // Act
        List<DonacionResponse> resultado = donacionService.getAll();

        // Assert
        assertNotNull(resultado);
        assertEquals(2, resultado.size());
        assertEquals("Ropa", resultado.get(0).getTipo());
        assertEquals("Alimentos", resultado.get(1).getTipo());
        verify(donacionRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getAll() con lista vacía debe retornar lista vacía")
    void getAll_listaVacia_debeRetornarListaVacia() {
        // Arrange
        when(donacionRepository.findAll()).thenReturn(List.of());

        // Act
        List<DonacionResponse> resultado = donacionService.getAll();

        // Assert
        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
        verify(donacionRepository, times(1)).findAll();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TEST 2: getByEstado()
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("getByEstado() con estado válido debe retornar donaciones filtradas")
    void getByEstado_estadoValido_debeRetornarDonacionesFiltradas() {
        // Arrange
        when(donacionRepository.findByEstado(Donacion.Estado.Recibida))
                .thenReturn(List.of(donacion1));

        // Act
        List<DonacionResponse> resultado = donacionService.getByEstado("Recibida");

        // Assert
        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals("Recibida", resultado.get(0).getEstado());
        verify(donacionRepository, times(1)).findByEstado(Donacion.Estado.Recibida);
    }

    @Test
    @DisplayName("getByEstado() con estado inválido debe lanzar IllegalArgumentException")
    void getByEstado_estadoInvalido_debeLanzarExcepcion() {
        // Arrange + Act + Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> donacionService.getByEstado("EstadoInexistente"));

        assertTrue(ex.getMessage().contains("Estado inválido"));
        verify(donacionRepository, never()).findByEstado(any());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TEST 3: getByUsuario()
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("getByUsuario() debe retornar donaciones del usuario")
    void getByUsuario_debeRetornarDonacionesDelUsuario() {
        // Arrange
        when(donacionRepository.findByUsuarioId(3L))
                .thenReturn(Arrays.asList(donacion1, donacion2));

        // Act
        List<DonacionResponse> resultado = donacionService.getByUsuario(3L);

        // Assert
        assertNotNull(resultado);
        assertEquals(2, resultado.size());
        verify(donacionRepository, times(1)).findByUsuarioId(3L);
    }

    @Test
    @DisplayName("getByUsuario() con usuario sin donaciones debe retornar lista vacía")
    void getByUsuario_sinDonaciones_debeRetornarListaVacia() {
        // Arrange
        when(donacionRepository.findByUsuarioId(99L)).thenReturn(List.of());

        // Act
        List<DonacionResponse> resultado = donacionService.getByUsuario(99L);

        // Assert
        assertTrue(resultado.isEmpty());
        verify(donacionRepository, times(1)).findByUsuarioId(99L);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TEST 4: getById()
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("getById() con id existente debe retornar la donacion")
    void getById_idExistente_debeRetornarDonacion() {
        // Arrange
        when(donacionRepository.findById(1L)).thenReturn(Optional.of(donacion1));

        // Act
        DonacionResponse resultado = donacionService.getById(1L);

        // Assert
        assertNotNull(resultado);
        assertEquals(1L, resultado.getId());
        assertEquals("Ropa", resultado.getTipo());
        assertEquals(50, resultado.getCantidad());
        verify(donacionRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("getById() con id inexistente debe lanzar IllegalArgumentException")
    void getById_idInexistente_debeLanzarExcepcion() {
        // Arrange
        when(donacionRepository.findById(999L)).thenReturn(Optional.empty());

        // Act + Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> donacionService.getById(999L));

        assertTrue(ex.getMessage().contains("999"));
        verify(donacionRepository, times(1)).findById(999L);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TEST 5: crear()
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("crear() con datos válidos debe guardar y retornar la donacion")
    void crear_datosValidos_debeGuardarYRetornarDonacion() {
        // Arrange
        DonacionRequest request = new DonacionRequest();
        request.setTipo("Higiene");
        request.setCantidad(100);
        request.setUnidad("kits");
        request.setOrigen("Particular");
        request.setCentroAcopio("Santiago Sur");
        request.setEstado("Recibida");
        request.setFecha(LocalDate.of(2025, 6, 1));
        request.setUsuarioId(1L);

        Donacion donacionGuardada = Donacion.builder()
                .id(3L)
                .tipo("Higiene")
                .cantidad(100)
                .unidad("kits")
                .origen("Particular")
                .centroAcopio("Santiago Sur")
                .estado(Donacion.Estado.Recibida)
                .fecha(LocalDate.of(2025, 6, 1))
                .usuarioId(1L)
                .build();

        when(donacionRepository.save(any(Donacion.class))).thenReturn(donacionGuardada);

        // Act
        DonacionResponse resultado = donacionService.crear(request);

        // Assert
        assertNotNull(resultado);
        assertEquals(3L, resultado.getId());
        assertEquals("Higiene", resultado.getTipo());
        assertEquals(100, resultado.getCantidad());
        verify(donacionRepository, times(1)).save(any(Donacion.class));
        verify(eventPublisher).publicarCreada(donacionGuardada);
    }

    @Test
    @DisplayName("actualizarEstado() publica evento cuando el estado cambia")
    void actualizarEstado_cambio_publicaEvento() {
        Donacion d = Donacion.builder().id(9L).tipo("Ropa").cantidad(1).unidad("u").origen("o")
                .centroAcopio("c").estado(Donacion.Estado.Recibida).build();
        when(donacionRepository.findById(9L)).thenReturn(Optional.of(d));
        when(donacionRepository.save(any(Donacion.class))).thenAnswer(i -> i.getArgument(0));

        donacionService.actualizarEstado(9L, "Distribuida");

        verify(eventPublisher).publicarEstadoCambiado(any(Donacion.class), eq(Donacion.Estado.Recibida));
    }

    @Test
    @DisplayName("actualizarEstado() no publica evento si el estado no cambia")
    void actualizarEstado_sinCambio_noPublica() {
        Donacion d = Donacion.builder().id(9L).tipo("Ropa").cantidad(1).unidad("u").origen("o")
                .centroAcopio("c").estado(Donacion.Estado.Recibida).build();
        when(donacionRepository.findById(9L)).thenReturn(Optional.of(d));
        when(donacionRepository.save(any(Donacion.class))).thenAnswer(i -> i.getArgument(0));

        donacionService.actualizarEstado(9L, "Recibida");

        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("crear() sin unidad debe usar 'unidades' por defecto")
    void crear_sinUnidad_debeUsarUnidadesPorDefecto() {
        // Arrange
        DonacionRequest request = new DonacionRequest();
        request.setTipo("Ropa");
        request.setCantidad(10);
        request.setUnidad(null);
        request.setOrigen("Particular");
        request.setCentroAcopio("Santiago");

        Donacion guardada = Donacion.builder()
                .id(4L).tipo("Ropa").cantidad(10).unidad("unidades")
                .origen("Particular").centroAcopio("Santiago")
                .estado(Donacion.Estado.Recibida).build();

        when(donacionRepository.save(any(Donacion.class))).thenReturn(guardada);

        // Act
        DonacionResponse resultado = donacionService.crear(request);

        // Assert
        assertEquals("unidades", resultado.getUnidad());
        verify(donacionRepository, times(1)).save(any(Donacion.class));
    }

    @Test
    @DisplayName("crear() con estado inválido debe lanzar IllegalArgumentException")
    void crear_estadoInvalido_debeLanzarExcepcion() {
        // Arrange
        DonacionRequest request = new DonacionRequest();
        request.setTipo("Ropa");
        request.setCantidad(10);
        request.setOrigen("Particular");
        request.setCentroAcopio("Santiago");
        request.setEstado("EstadoInvalido");

        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> donacionService.crear(request));
        verify(donacionRepository, never()).save(any());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TEST 6: actualizar()
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("actualizar() con id existente debe actualizar y retornar la donacion")
    void actualizar_idExistente_debeActualizarYRetornarDonacion() {
        // Arrange
        DonacionRequest request = new DonacionRequest();
        request.setTipo("Ropa Invierno");
        request.setCantidad(80);
        request.setUnidad("prendas");
        request.setOrigen("Empresa");
        request.setCentroAcopio("Santiago Centro");
        request.setEstado("En_transito");

        Donacion actualizada = Donacion.builder()
                .id(1L).tipo("Ropa Invierno").cantidad(80).unidad("prendas")
                .origen("Empresa").centroAcopio("Santiago Centro")
                .estado(Donacion.Estado.En_transito).build();

        when(donacionRepository.findById(1L)).thenReturn(Optional.of(donacion1));
        when(donacionRepository.save(any(Donacion.class))).thenReturn(actualizada);

        // Act
        DonacionResponse resultado = donacionService.actualizar(1L, request);

        // Assert
        assertNotNull(resultado);
        assertEquals("Ropa Invierno", resultado.getTipo());
        assertEquals(80, resultado.getCantidad());
        verify(donacionRepository, times(1)).findById(1L);
        verify(donacionRepository, times(1)).save(any(Donacion.class));
    }

    @Test
    @DisplayName("actualizar() con id inexistente debe lanzar IllegalArgumentException")
    void actualizar_idInexistente_debeLanzarExcepcion() {
        // Arrange
        when(donacionRepository.findById(999L)).thenReturn(Optional.empty());

        DonacionRequest request = new DonacionRequest();
        request.setTipo("X"); request.setCantidad(1);
        request.setOrigen("X"); request.setCentroAcopio("X");

        // Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> donacionService.actualizar(999L, request));
        verify(donacionRepository, never()).save(any());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TEST 7: actualizarEstado()
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("actualizarEstado() con estado válido debe cambiar el estado")
    void actualizarEstado_estadoValido_debeCambiarEstado() {
        // Arrange
        Donacion donacionActualizada = Donacion.builder()
                .id(1L).tipo("Ropa").cantidad(50).unidad("prendas")
                .origen("Particular").centroAcopio("Santiago Norte")
                .estado(Donacion.Estado.Distribuida).build();

        when(donacionRepository.findById(1L)).thenReturn(Optional.of(donacion1));
        when(donacionRepository.save(any(Donacion.class))).thenReturn(donacionActualizada);

        // Act
        DonacionResponse resultado = donacionService.actualizarEstado(1L, "Distribuida");

        // Assert
        assertNotNull(resultado);
        assertEquals("Distribuida", resultado.getEstado());
        verify(donacionRepository, times(1)).findById(1L);
        verify(donacionRepository, times(1)).save(any(Donacion.class));
    }

    @Test
    @DisplayName("actualizarEstado() con id inexistente debe lanzar excepcion")
    void actualizarEstado_idInexistente_debeLanzarExcepcion() {
        // Arrange
        when(donacionRepository.findById(999L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> donacionService.actualizarEstado(999L, "Distribuida"));
        verify(donacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("actualizarEstado() con estado inválido debe lanzar excepcion")
    void actualizarEstado_estadoInvalido_debeLanzarExcepcion() {
        // Arrange
        when(donacionRepository.findById(1L)).thenReturn(Optional.of(donacion1));

        // Act + Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> donacionService.actualizarEstado(1L, "EstadoFalso"));
        assertTrue(ex.getMessage().contains("Estado inválido"));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TEST 8: eliminar()
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("eliminar() con id existente debe eliminar la donacion")
    void eliminar_idExistente_debeEliminarDonacion() {
        // Arrange
        when(donacionRepository.existsById(1L)).thenReturn(true);
        doNothing().when(donacionRepository).deleteById(1L);

        // Act
        assertDoesNotThrow(() -> donacionService.eliminar(1L));

        // Assert
        verify(donacionRepository, times(1)).existsById(1L);
        verify(donacionRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("eliminar() con id inexistente debe lanzar IllegalArgumentException")
    void eliminar_idInexistente_debeLanzarExcepcion() {
        // Arrange
        when(donacionRepository.existsById(999L)).thenReturn(false);

        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> donacionService.eliminar(999L));
        verify(donacionRepository, never()).deleteById(any());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TEST 9: Validaciones adicionales de datos
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("getById() debe retornar datos correctos incluyendo usuarioId")
    void getById_debeRetornarDatosCompletos() {
        // Arrange
        when(donacionRepository.findById(1L)).thenReturn(Optional.of(donacion1));

        // Act
        DonacionResponse resultado = donacionService.getById(1L);

        // Assert
        assertAll("Datos completos de la donacion",
                () -> assertEquals(1L,             resultado.getId()),
                () -> assertEquals("Ropa",         resultado.getTipo()),
                () -> assertEquals(50,             resultado.getCantidad()),
                () -> assertEquals("prendas",      resultado.getUnidad()),
                () -> assertEquals("Particular",   resultado.getOrigen()),
                () -> assertEquals("Santiago Norte", resultado.getCentroAcopio()),
                () -> assertEquals(3L,             resultado.getUsuarioId())
        );
    }

    @Test
    @DisplayName("getByEstado() En_transito debe funcionar correctamente")
    void getByEstado_enTransito_debeRetornarDonaciones() {
        // Arrange
        when(donacionRepository.findByEstado(Donacion.Estado.En_transito))
                .thenReturn(List.of(donacion2));

        // Act
        List<DonacionResponse> resultado = donacionService.getByEstado("En_transito");

        // Assert
        assertEquals(1, resultado.size());
        assertEquals("En transito", resultado.get(0).getEstado());
    }

    @Test
    @DisplayName("crear() sin fecha debe usar fecha actual")
    void crear_sinFecha_debeUsarFechaActual() {
        // Arrange
        DonacionRequest request = new DonacionRequest();
        request.setTipo("Agua");
        request.setCantidad(20);
        request.setUnidad("litros");
        request.setOrigen("Empresa");
        request.setCentroAcopio("Temuco");
        request.setFecha(null);

        Donacion guardada = Donacion.builder()
                .id(5L).tipo("Agua").cantidad(20).unidad("litros")
                .origen("Empresa").centroAcopio("Temuco")
                .estado(Donacion.Estado.Recibida)
                .fecha(LocalDate.now()).build();

        when(donacionRepository.save(any(Donacion.class))).thenReturn(guardada);

        // Act
        DonacionResponse resultado = donacionService.crear(request);

        // Assert
        assertNotNull(resultado);
        assertEquals(LocalDate.now(), resultado.getFecha());
        verify(donacionRepository, times(1)).save(any(Donacion.class));
    }
}
