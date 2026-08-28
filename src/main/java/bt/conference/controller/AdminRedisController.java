package bt.conference.controller;

import bt.conference.dto.RedisAnalysisResponse;
import bt.conference.serviceinterface.IRedisAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin / Analysis Controller
 * Route: /v1/admin/redis-analysis?page=1&pageSize=20
 */
@RestController
@RequestMapping({"/v1/admin", "/api/admin", "/admin"})
public class AdminRedisController {
    @Autowired
    IRedisAnalysisService redisAnalysisService;

    /**
     * GET /redis-analysis
     * Query Params:
     *   - page: Page number (default 1)
     *   - pageSize: Page size (default 20, max 100)
     *   - limit: Optional override for pageSize
     *
     * Returns RedisAnalysisResponse matching:
     * export interface RedisAnalysisResponse {
     *     timestamp: string;
     *     pagination?: PaginationMeta;
     *     summary: PresenceSummary;
     *     users: PresenceUser[];
     *     serverInfo: ServerInfo;
     * }
     *
     * @param page     page number
     * @param pageSize page size
     * @param limit    optional limit overriding pageSize
     * @return ResponseEntity with RedisAnalysisResponse (200 OK), or 503 if unavailable, 500 on error
     */
    @GetMapping("/redis-analysis")
    public ResponseEntity<?> getRedisAnalysis(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(value = "limit", required = false) Integer limit
    ) {
        // If 'limit' query parameter is provided and > 0, override pageSize
        if (limit != null && limit > 0) {
            pageSize = limit;
        }

        // Validate and normalize page and pageSize bounds
        if (page <= 0) {
            page = 1;
        }
        if (pageSize <= 0) {
            pageSize = 20;
        }
        if (pageSize > 100) {
            pageSize = 100;
        }

        // Check if Redis is available (503 Service Unavailable if down)
        if (!redisAnalysisService.isRedisAvailable()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Redis is not available"));
        }

        try {
            RedisAnalysisResponse response = redisAnalysisService.getAnalysisReport(page, pageSize);
            return ResponseEntity.ok(response);
        } catch (Exception err) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", err.getMessage() != null ? err.getMessage() : "Unknown error occurred"));
        }
    }
}
