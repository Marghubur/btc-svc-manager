package bt.conference.controller;

import bt.conference.dto.*;
import bt.conference.entity.Conversation;
import bt.conference.service.ConversationService;
import com.fierhub.model.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "api/conversations/")
@RequiredArgsConstructor
public class ConversationController {
    private final ConversationService conversationService;

    /**
     * Get ALL conversations with pagination
     * GET /api/conversations?pageNumber=1&pageSize=10
     */
    @GetMapping("get-all")
    public BaseResponse getAllConversations(
            @RequestParam(defaultValue = "1") int pageNumber,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        PagedResponse<Conversation> response = conversationService
                .getAllConversations(pageNumber, pageSize);

        return BaseResponse.Ok(response);
    }

    /**
     * Get ALL conversations with pagination
     * GET /api/conversations?pageNumber=1&pageSize=10
     */
    @GetMapping("rooms")
    public BaseResponse getRooms(
            @RequestParam(defaultValue = "1") int pageNumber,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        PagedResponse<Conversation> response = conversationService
                .getRoomsService(pageNumber, pageSize);

        return BaseResponse.Ok(response);
    }

    /**
     * Search conversations by username, email, or conversation name
     * GET /api/conversations/search?term=john&pageNumber=1&pageSize=10
     */
    @GetMapping("search")
    public BaseResponse searchConversations(
            @RequestParam(required = false) String term,
            @RequestParam(defaultValue = "1") int pageNumber,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        PagedResponse<Conversation> response = conversationService
                .searchConversations(term, pageNumber, pageSize);

        return BaseResponse.Ok(response);
    }

    /**
     * Search conversations by username, email, or conversation name
     * GET /api/conversations/search?term=john&pageNumber=1&pageSize=10
     */
    @PostMapping("create/{id}")
    public BaseResponse createChannel(@PathVariable("id") String id, @RequestBody Conversation conversation) {
        Conversation response = conversationService.createSingleChannelService(id, conversation);
        return BaseResponse.Ok(response);
    }

    /**
     * Search conversations by username, email, or conversation name
     * POST /api/conversations/search?term=john&pageNumber=1&pageSize=10
     */
    @PostMapping("build-group/{id}")
    public BaseResponse buildGroupChannel(@PathVariable("id") String id, @RequestBody Conversation conversation) {
        Conversation response = conversationService.createGroupChannelService(id, conversation);
        return BaseResponse.Ok(response);
    }

    /**
     * Search conversations by username, email, or conversation name
     * POST /api/conversations/search?term=john&pageNumber=1&pageSize=10
     */
    @PostMapping("create-group/{userId}/{groupName}/{conversationId}")
    public BaseResponse createGroup(@PathVariable("groupName") String groupName,
                                   @PathVariable("userId") String userId,
                                   @PathVariable("conversationId") String conversationId,
                                   @RequestBody List<Conversation.Participant> groupUsers) {
        Conversation response = conversationService.createGroupService(userId, groupName, conversationId, groupUsers);
        return BaseResponse.Ok(response);
    }
}
