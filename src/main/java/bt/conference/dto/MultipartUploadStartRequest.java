package bt.conference.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MultipartUploadStartRequest {
    private String fileName;
    private String contentType;
    private String conversationId;
}
