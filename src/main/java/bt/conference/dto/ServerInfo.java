package bt.conference.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServerInfo {
    private String version;
    private String mode;
    private String os;
    private Long uptimeInSeconds;
    private Long uptimeInDays;
    private Integer connectedClients;
    private Long usedMemory;
    private String usedMemoryHuman;
    private Long peakMemory;
    private String peakMemoryHuman;
    private Long totalSystemMemory;
    private String totalSystemMemoryHuman;
    private Double memFragmentationRatio;
    private Long totalKeys;
    private Long instantaneousOpsPerSec;
}
