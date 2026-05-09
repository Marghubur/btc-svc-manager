package bt.conference.controller;

import bt.conference.model.GlobalSearchResponse;
import bt.conference.model.MessageSearchResult;
import bt.conference.service.MessageSearchService;
import com.fierhub.model.ApiErrorResponse;
import com.fierhub.model.BaseResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/messages/")
public class MessageSearchController {

    private static final Logger logger = LoggerFactory.getLogger(MessageSearchController.class);

    private final MessageSearchService messageSearchService;

    @Autowired
    public MessageSearchController(MessageSearchService messageSearchService) {
        this.messageSearchService = messageSearchService;
    }

    /**
     * Search only conversations/chats
     * GET /api/search/conversations?q=istiy&page=0&limit=20
     */
    @GetMapping("/conversations")
    public BaseResponse searchConversations(
            @RequestParam("q") String query,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {

        MessageSearchResult response = messageSearchService.searchMessagesService(query, page, limit);
        return BaseResponse.Ok(response);
    }

    /**
     * Search only conversations/chats
     * GET /api/search/messages?id=0000-0000-000-00000&page=0&limit=20
     */
    @GetMapping("get")
    public BaseResponse getMessages(
            @RequestParam("id") String query,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {

        MessageSearchResult response = messageSearchService.searchMessagesService(query, page, limit);
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