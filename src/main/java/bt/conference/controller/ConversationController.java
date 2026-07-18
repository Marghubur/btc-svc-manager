package bt.conference.controller;

import bt.conference.dto.*;
import bt.conference.entity.Conversation;
import bt.conference.model.CreateGroupRequest;
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
    @PutMapping("create/{id}/{recipientId}")
    public BaseResponse createChannel(@PathVariable("id") String id) throws Exception {
        Conversation response = conversationService.createSingleChannelService(id);
        return BaseResponse.Ok(response);
    }

    /**
     * Search conversations by username, email, or conversation name
     * POST /api/conversations/search?term=john&pageNumber=1&pageSize=10
     */
    @PostMapping("build-group/{id}")
    public BaseResponse buildGroupChannel(@PathVariable("id") String id, @RequestBody CreateGroupRequest groupRequest) throws Exception {
        Conversation response = conversationService.createGroupChannelService(id, groupRequest);
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

    /**
     * Add members to an existing group conversation
     * POST /api/conversations/add-members/{conversationId}?addedBy={userId}
     */
    @PostMapping("add-members/{conversationId}")
    public BaseResponse addMembersToGroup(@PathVariable("conversationId") String conversationId,
                                          @RequestParam("addedBy") String addedBy,
                                          @RequestBody List<String> userIds) {
        Conversation response = conversationService.addMembersToGroupService(conversationId, addedBy, userIds);
        return BaseResponse.Ok(response);
    }
}
