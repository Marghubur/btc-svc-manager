package bt.conference.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MultipartUploadCompleteRequest {
    private String fileKey;
    private String uploadId;
    private List<PartETag> parts;
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PartETag {
        private Integer partNumber;
        
        @JsonProperty("eTag")
        private String eTag;
    }
}
