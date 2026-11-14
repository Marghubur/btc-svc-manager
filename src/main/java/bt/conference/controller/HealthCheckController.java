package bt.conference.controller;

import com.fierhub.model.BaseResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/main/health")
public class HealthCheckController {

    @GetMapping(value = {"/healthCheck", "/check", ""})
    public ResponseEntity<BaseResponse> healthCheck() {
        Map<String, Object> healthData = new LinkedHashMap<>();
        healthData.put("status", "UP");
        healthData.put("service", "btc-main-service");
        healthData.put("timestamp", Instant.now().toString());

        return ResponseEntity.ok(BaseResponse.Ok(healthData));
    }

    @GetMapping(value = "/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("PONG");
    }
}
