package cl.donaciones.kafkalab.controller;

import cl.donaciones.kafkalab.consumer.SlowConsumer;
import cl.donaciones.kafkalab.service.LoadTestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Endpoints del lab 3.3.2 (tuning y pruebas de carga).
 *
 * <pre>
 * curl.exe -X POST "http://localhost:8888/api/test/start?count=1000"
 * curl.exe "http://localhost:8888/api/test/config"
 * </pre>
 */
@RestController
@RequestMapping("/api/test")
public class LoadTestController {

    static final int MAX_COUNT = 1_000_000;

    private final LoadTestService service;
    private final SlowConsumer slowConsumer;

    public LoadTestController(LoadTestService service, SlowConsumer slowConsumer) {
        this.service = service;
        this.slowConsumer = slowConsumer;
    }

    /** POST /api/test/start?count=N : envía N eventos y espera la confirmación de cada uno. */
    @PostMapping("/start")
    public ResponseEntity<String> start(@RequestParam(defaultValue = "1000") int count) {
        if (count < 1 || count > MAX_COUNT) {
            return ResponseEntity.badRequest()
                    .body("count debe estar entre 1 y " + MAX_COUNT + "\n");
        }
        return ResponseEntity.ok(service.run(count).resumen() + "\n");
    }

    /** GET /api/test/config : escenario activo del productor y avance del SlowConsumer. */
    @GetMapping("/config")
    public Map<String, Object> config() {
        Map<String, Object> body = new LinkedHashMap<>(service.producerConfig());
        body.put("consumidos.analytics-group", slowConsumer.getProcesados());
        return body;
    }
}
