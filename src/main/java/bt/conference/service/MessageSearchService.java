package bt.conference.service;

import bt.conference.entity.Conversation;
import bt.conference.model.MessageSearchResult;
import bt.conference.repository.ConversationRepository;
import bt.conference.repository.MessageSearchRepository;
import bt.conference.repository.MessageSearchRepository.MessageSearchCriteria;
import bt.conference.repository.UserPresenceRedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MessageSearchService {

    private final MessageSearchRepository messageRepository;
    private final UserPresenceRedisRepository presenceRepository;
    private final ConversationRepository conversationRepository;

    /**
     * Search a message
     */
    public Map<String, Object> searchMessagesService(String conId, int page, int limit) {
        if (!validateSearchCriteria(conId, page, limit)) {
            Map<String, Object> emptyResult = new HashMap<>();
            emptyResult.put("searchResult", MessageSearchResult.empty());
            emptyResult.put("userStatus", new HashMap<>());
            return emptyResult;
        }

        MessageSearchCriteria criteria = MessageSearchCriteria.builder()
                .conversationId(conId)
                .skip((page - 1) * limit)
                .limit(limit)
                .build();

        MessageSearchResult searchResult = this.messageRepository.searchMessages(criteria);

        Conversation conversation = conversationRepository.findById(conId).orElse(null);

        Set<String> userIdsToFetch = new HashSet<>();

        if (conversation != null && conversation.getParticipantIds() != null) {
                var teamMembersId = conversation.getParticipantIds()
                                    .stream()
                                    .filter(x -> !x.equals(conversation.getCreatedBy()))
                                    .limit(10)
                                    .toList();
                userIdsToFetch.addAll(teamMembersId);
        }

        Map<String, Object> userStatuses = presenceRepository.getPresenceForUsers(userIdsToFetch);

        Map<String, Object> finalResult = new HashMap<>();
        finalResult.put("searchResult", searchResult);
        finalResult.put("userStatus", userStatuses);

        return finalResult;
    }

    private boolean validateSearchCriteria(String conId, int page, int limit) {
        if (conId == null || conId.isEmpty()) {
            return false;
        }

        // At least one filter must be provided
        boolean hasFilter = page > 0 ||
                limit >= 10;

        if (!hasFilter) {
            throw new GlobalSearchException(GlobalSearchException.ErrorType.INVALID_INPUT,
                    "At least one search filter is required");
        }

        return true;
    }
}
