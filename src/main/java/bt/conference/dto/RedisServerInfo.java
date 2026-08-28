package bt.conference.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedisServerInfo {
    private String redisVersion;
    private String redisMode;
    private String os;
    private Long uptimeInSeconds;
    private Long uptimeInDays;
    private Integer connectedClients;
    private Long totalConnectionsReceived;
    private Long totalCommandsProcessed;
    private Long instantaneousOpsPerSec;
}
