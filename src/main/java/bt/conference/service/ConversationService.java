package bt.conference.service;

import bt.conference.dto.*;
import bt.conference.entity.Conversation;
import bt.conference.entity.Conversation.Participant;
import bt.conference.entity.ConversationMembers;
import bt.conference.entity.Users;
import bt.conference.repository.ConversationMembersRepository;
import bt.conference.repository.ConversationRepository;
import bt.conference.repository.UsersRepository;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fierhub.model.UserSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;
import java.util.logging.Logger;

import java.time.Instant;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConversationService {
    private final ConversationRepository conversationRepository;
    private final ConversationMembersRepository conversationMembersRepository;
    private final UsersRepository usersRepository;
    private final MongoTemplate mongoTemplate;
    private final UserSession userSession;

    private static final Logger logger = Logger.getLogger(ObjectIdGenerators.UUIDGenerator.class.getName());

    /**
     * Get ALL conversations with pagination (No filter)
     */
    public PagedResponse<Conversation> getAllConversations(int pageNumber, int pageSize) {

        Pageable pageable = PageRequest.of(
                pageNumber - 1,  // Spring uses 0-indexed pages
                pageSize,
                Sort.by(Sort.Direction.DESC, "lastMessageAt")
        );

        Page<Conversation> page = conversationRepository.findAll(pageable);

        return PagedResponse.of(
                page.getContent(),
                page.getTotalPages(),
                pageNumber,
                pageSize
        );
    }

    /**
     * Get ALL conversations that contains current user id with pagination (No filter)
     */
    public PagedResponse<Conversation> getRoomsService(
            int pageNumber,
            int pageSize) {
        
        List<ConversationMembers> memberships = conversationMembersRepository.findByUserId(this.userSession.getUserId());
        List<String> conversationIds = memberships.stream()
                .map(ConversationMembers::getConversationId)
                .toList();

        if (conversationIds.isEmpty()) {
            return PagedResponse.of(
                    Collections.emptyList(),
                    0,
                    pageNumber,
                    pageSize
            );
        }

        Pageable pageable = PageRequest.of(
                pageNumber - 1,
                pageSize
        );

        Page<Conversation> page = conversationRepository.findByIdInAndIsDeletedFalseOrderByLastMessageAtDesc(conversationIds, pageable);

        return PagedResponse.of(
                page.getContent(),
                page.getTotalPages(),
                pageNumber,
                pageSize
        );
    }

    /**
     * Get ALL conversations using MongoTemplate (Alternative)
     */
    public PagedResponse<Conversation> getAllConversationsWithTemplate(int pageNumber, int pageSize) {

        int skip = (pageNumber - 1) * pageSize;

        // Count total
        long totalRecords = mongoTemplate.count(new Query(), Conversation.class);

        // Fetch with pagination
        Query query = new Query()
                .with(Sort.by(Sort.Direction.DESC, "lastMessageAt"))
                .skip(skip)
                .limit(pageSize);

        List<Conversation> conversations = mongoTemplate.find(query, Conversation.class);

        int totalPages = (int) Math.ceil((double) totalRecords / pageSize);

        return PagedResponse.of(
                conversations,
                totalPages,
                pageNumber,
                pageSize
        );
    }

    /**
     * Search conversations by term (username, email, conversation_name)
     */
    public PagedResponse<Conversation> searchConversationsRecentGroup(
            String searchTerm,
            int pageNumber,
            int pageSize
    ) {
        int skip = (pageNumber - 1) * pageSize;

        Query query = new Query();
        query.addCriteria(Criteria.where("type").is("GROUP"));
        query.addCriteria(Criteria.where("isDeleted").is(false));

        // Add search filter if term provided
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            String pattern = searchTerm.trim();

            // Find users matching search term
            Query userQuery = new Query(new Criteria().orOperator(
                    Criteria.where("username").regex(pattern, "i"),
                    Criteria.where("email").regex(pattern, "i"),
                    Criteria.where("firstName").regex(pattern, "i"),
                    Criteria.where("lastName").regex(pattern, "i")
            ));
            List<Users> users = mongoTemplate.find(userQuery, Users.class);
            List<String> userIds = users.stream().map(Users::getId).toList();

            // Find memberships
            List<ConversationMembers> memberships = mongoTemplate.find(
                    new Query(Criteria.where("userId").in(userIds)),
                    ConversationMembers.class
            );
            List<String> conversationIds = memberships.stream()
                    .map(ConversationMembers::getConversationId)
                    .toList();

            query.addCriteria(Criteria.where("id").in(conversationIds));
        }

        // Count total matching records
        long totalRecords = mongoTemplate.count(query, Conversation.class);

        log.info("Search term: '{}', Total records found: {}", searchTerm, totalRecords);

        // Add sorting and pagination
        query.with(Sort.by(Sort.Direction.DESC, "lastMessageAt"));
        query.skip(skip);
        query.limit(pageSize);

        // Execute query
        List<Conversation> conversations = mongoTemplate.find(query, Conversation.class);

        log.info("Returning {} conversations", conversations.size());

        int totalPages = (int) Math.ceil((double) totalRecords / pageSize);

        return PagedResponse.of(
                conversations,
                totalPages,
                pageNumber,
                pageSize
        );
    }

    /**
     * Search conversations by term (username, email, conversation_name)
     */
    public PagedResponse<Conversation> searchConversations(
            String searchTerm,
            int pageNumber,
            int pageSize
    ) {
        int skip = (pageNumber - 1) * pageSize;

        Query query = new Query();
        query.addCriteria(Criteria.where("isDeleted").is(false));

        // Add search filter if term provided
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            String pattern = searchTerm.trim();

            // Find users matching search term
            Query userQuery = new Query(new Criteria().orOperator(
                    Criteria.where("username").regex(pattern, "i"),
                    Criteria.where("email").regex(pattern, "i"),
                    Criteria.where("firstName").regex(pattern, "i"),
                    Criteria.where("lastName").regex(pattern, "i")
            ));
            List<Users> users = mongoTemplate.find(userQuery, Users.class);
            List<String> userIds = users.stream().map(Users::getId).toList();

            // Find memberships
            List<ConversationMembers> memberships = mongoTemplate.find(
                    new Query(Criteria.where("userId").in(userIds)),
                    ConversationMembers.class
            );
            List<String> conversationIds = memberships.stream()
                    .map(ConversationMembers::getConversationId)
                    .toList();

            Criteria searchCriteria = new Criteria().orOperator(
                    Criteria.where("title").regex(pattern, "i"),
                    Criteria.where("description").regex(pattern, "i"),
                    Criteria.where("id").in(conversationIds)
            );

            query.addCriteria(searchCriteria);
        }

        // Count total matching records
        long totalRecords = mongoTemplate.count(query, Conversation.class);

        log.info("Search term: '{}', Total records found: {}", searchTerm, totalRecords);

        // Add sorting and pagination
        query.with(Sort.by(Sort.Direction.DESC, "lastMessageAt"));
        query.skip(skip);
        query.limit(pageSize);

        // Execute query
        List<Conversation> conversations = mongoTemplate.find(query, Conversation.class);

        log.info("Returning {} conversations", conversations.size());

        int totalPages = (int) Math.ceil((double) totalRecords / pageSize);

        return PagedResponse.of(
                conversations,
                totalPages,
                pageNumber,
                pageSize
        );
    }

    /**
     * Search conversations by term (username, email, conversation_name)
     */
    public Conversation createSingleChannelService(String recipientId, Conversation conversation) {
        // Validate: Check only two participants for direct chat
        if (recipientId == null || conversation.getCreatedBy() == null || conversation.getCreatedBy().isEmpty()) {
            throw new IllegalArgumentException("Cannot create conversation, required sender and receiver detail");
        }

        return createConversationService(recipientId, "direct", conversation);
    }

    /**
     * Search conversations by term (username, email, conversation_name)
     */
    public Conversation createGroupChannelService(String senderId, Conversation conversation) {
        return createConversationService(senderId, "group", conversation);
    }

    private void validateGroupParticipants(List<Participant> participants) {
        if (participants == null || participants.size() < 2) {
            throw new IllegalArgumentException("At least two participants are required to create a group");
        }

        participants.stream().filter(x -> x.getUserId() == null || x.getUserId().isEmpty()).findFirst()
                .ifPresent(x -> {
                    throw new IllegalArgumentException("Participant userId should not be empty or null");
                });
    }

    public Conversation createGroupService(String userId, String groupName, String conversationId, List<Participant> participants) {
        if (groupName == null || groupName.isEmpty()) {
            throw new IllegalArgumentException("Group name should not be empty or null");
        }

        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("User id should not be empty or null");
        }


        Optional<Users> userCache = this.usersRepository.findById(userSession.getUserId());
        var currentUser = userCache.stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userSession.getUserId()));

        Instant now = Instant.now();
        participants.add(Participant.builder()
                .userId(currentUser.getId())
                .firstName(currentUser.getFirstName())
                .lastName(currentUser.getLastName())
                .email(currentUser.getEmail())
                .avatar(currentUser.getAvatarUrl())
                .joinedAt(now)
                .role("User")
                .build());

        validateGroupParticipants(participants);

        var owner = participants.stream().filter(x -> x.getUserId().equals(userId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("At least one admin is required in group"));

        owner.setRole("admin");

        // Build conversation
        Conversation conversationInstance = Conversation.builder()
                .type("GROUP")
                .title(groupName)
                .avatar(null)
                .createdBy(owner.getUserId())
                .createdAt(now)
                .lastMessageAt(now)
                .isDeleted(false)
                .memberCount(participants.size())
                .settings(Conversation.ConversationSettings.builder()
                        .allowReactions(true)
                        .allowPinning(true)
                        .adminOnlyPost(false)
                        .build())
                .build();

        if (isValidObjectIdHex(conversationId)) {
            conversationInstance.setId(conversationId);
        } else {
            var first = participants.stream().findAny();
            if (first.isPresent()) {
                conversationInstance.setId(generateMongoObjectId(userId, first.get().getUserId()));
            } else {
                conversationInstance.setId(generateMongoObjectId(userId, "empty"));
            }
        }

        // Save to database
        Conversation saved = conversationRepository.save(conversationInstance);

        // Save members in conversation_members
        for (Participant p : participants) {
            ConversationMembers member = ConversationMembers.builder()
                    .id(new ObjectId().toHexString())
                    .conversationId(saved.getId())
                    .userId(p.getUserId())
                    .role("admin".equalsIgnoreCase(p.getRole()) ? "ADMIN" : "MEMBER")
                    .joinedAt(now)
                    .joinedBy(owner.getUserId())
                    .status("ACTIVE")
                    .unreadCount(0)
                    .isMuted(false)
                    .isPinned(false)
                    .isArchived(false)
                    .notification("ALL")
                    .nickname(p.getFirstName() + "_" + p.getLastName())
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            conversationMembersRepository.save(member);
        }

        log.info("Created new group conversation: {}", saved.getId());

        return saved;
    }

    public static boolean isValidObjectIdHex(String hex) {
        if (hex == null || hex.isBlank()) {
            return false;
        }

        return hex.matches("^[a-fA-F0-9]{24}$");
    }

    public String generateUUID(String firstUserId, String secondUserId) {
        // Sort both IDs lexicographically
        String id1;
        String id2;

        if (firstUserId.compareTo(secondUserId) <= 0) {
            id1 = firstUserId;
            id2 = secondUserId;
        } else {
            id1 = secondUserId;
            id2 = firstUserId;
        }

        // Concatenate after sorting
        String value = id1 + id2;

        // Fixed namespace
        UUID namespace = UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8");

        // Deterministic UUID
        byte[] nameBytes = (namespace.toString() + value)
                .getBytes(StandardCharsets.UTF_8);

        UUID clientID = UUID.nameUUIDFromBytes(nameBytes);

        logger.info(clientID.toString());

        return clientID.toString();
    }

    public String generateMongoObjectId(String firstUserId, String secondUserId) {
        try {
            // Generate deterministic UUID
            String uuid = generateUUID(firstUserId, secondUserId);

            // SHA-1 hash of UUID
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] hash = sha1.digest(uuid.getBytes(StandardCharsets.UTF_8));

            // Take first 12 bytes
            byte[] objectIdBytes = new byte[12];
            System.arraycopy(hash, 0, objectIdBytes, 0, 12);

            return new ObjectId(objectIdBytes).toHexString();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate MongoDB ObjectId", e);
        }
    }

    private Conversation createConversationService(String senderId, String type, Conversation conversation) {
        String receiverId;

        Optional<String> filterSender = conversation.getParticipantIds()
                .stream()
                .filter(x -> x.equals(senderId))
                .findFirst();

        if (filterSender.isEmpty()) {
            throw new IllegalArgumentException("Sender id not found in participantIds");
        }

        Optional<String> filterReceiver = conversation.getParticipantIds()
                .stream()
                .filter(x -> !x.equals(senderId))
                .findFirst();

        if (filterReceiver.isEmpty()) {
            throw new IllegalArgumentException("Receiver id not found in participantIds");
        }

        receiverId = filterReceiver.get();

        // Get user details
        Users sender = usersRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Current user not found: " + senderId));

        Users receiver = usersRepository.findById(receiverId)
                .orElseThrow(() -> new RuntimeException("Other user not found: " + receiverId));

        // Validate
        if (sender.getId().equals(receiver.getId())) {
            throw new IllegalArgumentException("Cannot create conversation with yourself");
        }

        log.info("Creating direct conversation between {} and {}", sender.getId(), receiver.getId());

        // Check if direct conversation already exists
        Optional<Conversation> existing = conversationRepository.findDirectConversation(sender.getId(), receiver.getId());

        if (existing.isPresent()) {
            log.info("Direct conversation already exists: {}", existing.get().getId());
            return existing.get();
        }

        // Build conversation
        Instant now = Instant.now();
        String conversationIdHex = generateMongoObjectId(sender.getId(), receiver.getId());

        Conversation conversationInstance = Conversation.builder()
                .id(conversationIdHex)
                .type(type.toUpperCase())
                .title("")  // Direct chats don't have title
                .avatar(null)
                .createdBy(senderId)
                .createdAt(now)
                .lastMessageAt(now)
                .isDeleted(false)
                .memberCount(2)
                .settings(Conversation.ConversationSettings.builder()
                        .allowReactions(true)
                        .allowPinning(true)
                        .adminOnlyPost(false)
                        .build())
                .build();

        // Save to database
        Conversation saved = conversationRepository.save(conversationInstance);

        // Save members in conversation_members
        ConversationMembers memberSender = ConversationMembers.builder()
                .id(new ObjectId().toHexString())
                .conversationId(saved.getId())
                .userId(sender.getId())
                .role("ADMIN")
                .joinedAt(now)
                .joinedBy(senderId)
                .status("ACTIVE")
                .unreadCount(0)
                .isMuted(false)
                .isPinned(false)
                .isArchived(false)
                .notification("ALL")
                .nickname(sender.getUsername())
                .createdAt(now)
                .updatedAt(now)
                .build();
        conversationMembersRepository.save(memberSender);

        ConversationMembers memberReceiver = ConversationMembers.builder()
                .id(new ObjectId().toHexString())
                .conversationId(saved.getId())
                .userId(receiver.getId())
                .role("MEMBER")
                .joinedAt(now)
                .joinedBy(senderId)
                .status("ACTIVE")
                .unreadCount(0)
                .isMuted(false)
                .isPinned(false)
                .isArchived(false)
                .notification("ALL")
                .nickname(receiver.getUsername())
                .createdAt(now)
                .updatedAt(now)
                .build();
        conversationMembersRepository.save(memberReceiver);

        log.info("Created new direct conversation: {}", saved.getId());

        return saved;
    }

    public void updateLastMessage(String conversationId, Conversation.LastMessage lastMessage) {
        Optional<Conversation> convOpt = conversationRepository.findById(conversationId);
        if (convOpt.isPresent()) {
            Conversation conv = convOpt.get();
            conv.setLastMessageId(lastMessage.getMessageId());
            conv.setLastMessageAt(lastMessage.getSentAt());
            conversationRepository.save(conv);
        }
    }
}
