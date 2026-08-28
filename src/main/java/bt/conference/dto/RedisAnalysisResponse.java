package bt.conference.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Matching TypeScript interface:
 * export interface RedisAnalysisResponse {
 *     timestamp: string;
 *     pagination?: PaginationMeta;
 *     summary: PresenceSummary;
 *     users: PresenceUser[];
 *     serverInfo: ServerInfo;
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RedisAnalysisResponse {
    private String timestamp;
    private PaginationMeta pagination;
    private PresenceSummary summary;
    private List<PresenceUser> users;
    private ServerInfo serverInfo;
}
