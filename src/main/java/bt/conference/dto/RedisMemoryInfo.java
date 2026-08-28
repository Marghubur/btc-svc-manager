package bt.conference.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedisMemoryInfo {
    private Long usedMemoryBytes;
    private String usedMemoryHuman;
    private Long usedMemoryPeakBytes;
    private String usedMemoryPeakHuman;
    private Long totalSystemMemoryBytes;
    private String totalSystemMemoryHuman;
    private Double memFragmentationRatio;
    private String maxMemoryPolicy;
}
