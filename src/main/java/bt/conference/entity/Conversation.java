package bt.conference.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "conversations")
public class Conversation {

    @Id
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

    // Transient fields for API payloads/compatibility
    @Transient
    private List<String> participantIds;

    @Transient
    private List<Participant> participants;

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