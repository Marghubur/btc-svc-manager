package bt.conference.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PresenceSession {
    private String sessionId;
    private String status;
    private Object lastSeen;
    private String client;
    private String device;
    private Double expiresAt;
    private Map<String, Object> metadata;
}
