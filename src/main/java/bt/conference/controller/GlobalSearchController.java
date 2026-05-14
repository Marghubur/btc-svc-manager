package bt.conference.controller;

import bt.conference.model.GlobalSearchResponse;
import bt.conference.service.GlobalSearchService;

import com.fierhub.model.ApiErrorResponse;
import com.fierhub.model.BaseResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/search/")
public class GlobalSearchController {

    private static final Logger logger = LoggerFactory.getLogger(GlobalSearchController.class);

    private final GlobalSearchService searchService;

    @Autowired
    public GlobalSearchController(GlobalSearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * Health check
     * GET /api/search/health
     */
    @GetMapping("health/")
    public BaseResponse health() {
        boolean healthy = searchService.isHealthy();
        if (healthy) {
            return BaseResponse.Ok(Map.of(
                    "status", "UP",
                    "service", "GlobalSearchService"
            ));
        }
        return ApiErrorResponse.RaiseError(Map.of(
                "status", "DOWN",
                "service", "GlobalSearchService"
        ), HttpStatus.SERVICE_UNAVAILABLE, "Search service is currently unavailable");
    }

    /**
     * Metrics for monitoring
     * GET /api/search/metrics
     */
    @GetMapping("metrics/")
    public BaseResponse metrics() {
        return BaseResponse.Ok(searchService.getMetrics());
    }

    /**
     * Typeahead search - use as user types (debounce on frontend!)
     * GET /api/search/typeahead?q=ist
     * <p>
     * Returns limited results (5 per category) for quick display
     */
    @GetMapping("/typeahead")
    public BaseResponse typeahead(
            @RequestParam("q") String query,
            @RequestParam("fs") String fullSearch
    ) {
        GlobalSearchResponse response = searchService.typeahead(query, fullSearch);
        return buildResponse(response);
    }

    /**
     * Full global search with pagination
     * GET /api/search/global?q=istiy&page=0&limit=20
     */
    @GetMapping("/global")
    public BaseResponse globalSearch(
            @RequestParam("q") String query,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            Principal principal) {

        String userId = principal != null ? principal.getName() : null;
        logger.debug("Global search: query='{}', page={}, limit={}, user='{}'", query, page, limit, userId);

        GlobalSearchResponse response = searchService.search(query, userId, page, limit);
        return buildResponse(response);
    }

    /**
     * Search only users/people
     * GET /api/search/users?q=istiy&page=0&limit=20
     */
    @GetMapping("/users")
    public BaseResponse searchUsers(
            @RequestParam("q") String query,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            Principal principal) {

        String userId = principal != null ? principal.getName() : null;
        GlobalSearchResponse response = searchService.searchUsers(query, userId, page, limit);
        return buildResponse(response);
    }

    /**
     * Search only conversations/chats
     * GET /api/search/conversations?q=istiy&page=0&limit=20
     */
    @GetMapping("/conversations")
    public BaseResponse searchConversations(
            @RequestParam("q") String query,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            Principal principal) {

        String userId = principal != null ? principal.getName() : null;
        GlobalSearchResponse response = searchService.searchConversations(query, userId, page, limit);
        return buildResponse(response);
    }

    /**
     * Build appropriate response based on error state
     */
    private BaseResponse buildResponse(GlobalSearchResponse response) {
        if (response.hasError()) {
            return ApiErrorResponse.RaiseError(
                    response,
                    HttpStatus.valueOf(response.getError().getCode()),
                    response.getError().getMessage()
            );
        }
        return BaseResponse.Ok(response);
    }

    /**
     * Global exception handler for this controller
     */
    @ExceptionHandler(Exception.class)
    public BaseResponse handleException(Exception ex) {
        logger.error("Unhandled exception in search controller: {}", ex.getMessage(), ex);
        return ApiErrorResponse.RaiseError(
                GlobalSearchResponse.error("INTERNAL_ERROR", "An unexpected error occurred"),
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred while processing the search request"
        );
    }
}