package bt.conference.dto;

import lombok.Data;

@Data
public class PresignedUrlRequest {
    private String fileName;
    private String contentType;
    private String conversationId;
}
