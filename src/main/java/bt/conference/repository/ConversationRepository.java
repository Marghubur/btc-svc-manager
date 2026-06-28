package bt.conference.repository;

import bt.conference.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends MongoRepository<Conversation, String> {

    // Find conversations by list of IDs ordered by lastMessageAt descending
    Page<Conversation> findByIdInAndIsDeletedFalseOrderByLastMessageAtDesc(List<String> ids, Pageable pageable);

    List<Conversation> findByIdInAndIsDeletedFalseOrderByLastMessageAtDesc(List<String> ids);

    // Search by title or description
    @Query("{ " +
            "  '_id': { $in: ?0 }, " +
            "  'isDeleted': false, " +
            "  $or: [ " +
            "    { 'title': { $regex: ?1, $options: 'i' } }, " +
            "    { 'description': { $regex: ?1, $options: 'i' } } " +
            "  ] " +
            "}")
    Page<Conversation> searchConversations(List<String> ids, String searchTerm, Pageable pageable);

    @Query(value = "{ " +
            "  '_id': { $in: ?0 }, " +
            "  'isDeleted': false, " +
            "  $or: [ " +
            "    { 'title': { $regex: ?1, $options: 'i' } }, " +
            "    { 'description': { $regex: ?1, $options: 'i' } } " +
            "  ] " +
            "}", count = true)
    long countSearchResults(List<String> ids, String searchTerm);

    // Get meeting by id
    @Query("{ '_id': ?0 }")
    Optional<Conversation> getMeetingById(String meetingId);

    // Check if direct conversation exists between two users
    @Aggregation(pipeline = {
            "{ $match: { 'type': 'DIRECT', 'isDeleted': false } }",
            "{ $lookup: { from: 'conversation_members', localField: '_id', foreignField: 'conversationId', as: 'members' } }",
            "{ $match: { 'members.userId': { $all: [?0, ?1] } } }"
    })
    Optional<Conversation> findDirectConversation(String userId1, String userId2);
}
