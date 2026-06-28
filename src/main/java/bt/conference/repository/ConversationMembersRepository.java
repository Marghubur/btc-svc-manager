package bt.conference.repository;

import bt.conference.entity.ConversationMembers;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationMembersRepository extends MongoRepository<ConversationMembers, String> {
    List<ConversationMembers> findByUserId(String userId);
    List<ConversationMembers> findByConversationId(String conversationId);
    Optional<ConversationMembers> findByConversationIdAndUserId(String conversationId, String userId);
    boolean existsByConversationIdAndUserId(String conversationId, String userId);
}
