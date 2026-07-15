package bt.conference.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.MongoId;
import org.springframework.data.mongodb.core.mapping.FieldType;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "conversations")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Conversation {

    @Id
    @MongoId(FieldType.STRING)
    private String id;

    @Field("avatar")
    private String avatar;

    @Field("createdAt")
    private Instant createdAt;

    @Field("createdBy")
    private String createdBy;

    @Field("description")
    private String description;

    @Field("isDeleted")
    private boolean isDeleted;

    @Field("lastMessageAt")
    private Instant lastMessageAt;

    @Field("lastMessageId")
    private String lastMessageId;

    @Field("memberCount")
    private int memberCount;

    @Field("settings")
    private ConversationSettings settings;

    @Field("title")
    private String title;

    @Field("type")
    private String type;

    @Field("searchableMemberInfo")
    private List<String> searchableMemberInfo;

    @Field("participant_ids")
    private List<String> participantIds;

    // Transient fields for API payloads/compatibility

    @Transient
    private List<Participant> participants;

    @JsonProperty("conversation_id")
    public String getConversationId() {
        return id;
    }

    @JsonProperty("last_message_at")
    public Long getLastMessageAtMillis() {
        return lastMessageAt != null ? lastMessageAt.toEpochMilli() : null;
    }

    @JsonProperty("member_count")
    public int getMemberCountSnakeCase() {
        return memberCount;
    }

    @JsonProperty("members")
    public List<Participant> getMembers() {
        return participants != null ? participants : Collections.emptyList();
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Participant {
        private String userId;
        private String firstName;
        private String lastName;
        private String email;
        private String avatar;
        private Instant joinedAt;
        private String role;
        private String status;

        @JsonProperty("user_id")
        public String getUserIdSnake() { return userId; }

        @JsonProperty("first_name")
        public String getFirstNameSnake() { return firstName; }

        @JsonProperty("last_name")
        public String getLastNameSnake() { return lastName; }

        @JsonProperty("joined_at")
        public Long getJoinedAtMillis() { return joinedAt != null ? joinedAt.toEpochMilli() : null; }
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LastMessage {
        private String messageId;
        private String content;
        private String senderId;
        private String senderName;
        private Instant sentAt;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ConversationSettings {
        @Field("allowReactions")
        private boolean allowReactions;

        @Field("allowPinning")
        private boolean allowPinning;

        @Field("adminOnlyPost")
        private boolean adminOnlyPost;
    }
}