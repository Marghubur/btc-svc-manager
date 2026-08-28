package bt.conference.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedisAnalysisReport {
    private String status;
    private String generatedAt;
    private RedisServerInfo server;
    private RedisMemoryInfo memory;
    private Long totalKeys;
    private Map<String, Long> keyCountByType;
    private int page;
    private int pageSize;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;
    private List<RedisKeyDetail> keys;
}
