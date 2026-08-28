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
public class PresenceSummary {
    private long totalUsers;
    private long online;
    private long offline;
    private long away;
    private long busy;
    private long activeSessions;
}
