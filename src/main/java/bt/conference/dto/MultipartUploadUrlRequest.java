package bt.conference.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MultipartUploadUrlRequest {
    private String fileKey;
    private String uploadId;
    private Integer partNumber;
}
