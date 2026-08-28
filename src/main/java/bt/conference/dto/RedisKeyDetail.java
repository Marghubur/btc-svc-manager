package bt.conference.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedisKeyDetail {
    private String key;
    private String type;
    private Long ttlSeconds;
    private Long memoryUsageBytes;
    private String memoryUsageHuman;
}
