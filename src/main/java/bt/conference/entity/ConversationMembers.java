package bt.conference.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "conversation_members")
public class ConversationMembers {

    @Id
    private String id;

    @Field("conversationId")
    private String conversationId;

    @Field("userId")
    private String userId;

    @Field("role")
    private String role;

    @Field("joinedAt")
    private Instant joinedAt;

    @Field("joinedBy")
    private String joinedBy;

    @Field("status")
    private String status;

    @Field("lastReadMessageId")
    private String lastReadMessageId;

    @Field("lastReadAt")
    private Instant lastReadAt;

    @Field("unreadCount")
    private int unreadCount;

    @Field("isMuted")
    private boolean isMuted;

    @Field("muteUntil")
    private Instant muteUntil;

    @Field("isPinned")
    private boolean isPinned;

    @Field("isArchived")
    private boolean isArchived;

    @Field("notification")
    private String notification;

    @Field("nickname")
    private String nickname;

    @Field("leftAt")
    private Instant leftAt;

    @Field("removedAt")
    private Instant removedAt;

    @Field("removedBy")
    private String removedBy;

    @Field("createdAt")
    private Instant createdAt;

    @Field("updatedAt")
    private Instant updatedAt;
}
