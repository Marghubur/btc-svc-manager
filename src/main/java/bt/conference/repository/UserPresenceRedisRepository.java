package bt.conference.repository;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Repository
public class UserPresenceRedisRepository {

    private final StringRedisTemplate redisTemplate;

    public UserPresenceRedisRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Fetches user presence (status and last_seen) for a set of user IDs.
     * @param userIds Set of user IDs to fetch presence for.
     * @return Map where the key is the user ID and the value is a map of status properties.
     */
    public Map<String, Object> getPresenceForUsers(Set<String> userIds) {
        Map<String, Object> statuses = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) {
            return statuses;
        }

        double now = Instant.now().getEpochSecond();

        for (String uid : userIds) {
            String sessionsKey = "presence:user:" + uid + ":sessions";
            
            // Check active sessions in the sorted set
            Set<String> sessions = redisTemplate.opsForZSet().rangeByScore(sessionsKey, now, Double.MAX_VALUE);
            
            if (sessions != null && !sessions.isEmpty()) {
                // Pick the first active session
                String firstSession = sessions.iterator().next();
                String sessionKey = "presence:session:" + firstSession;
                
                // Fetch the session metadata
                Map<Object, Object> sessionData = redisTemplate.opsForHash().entries(sessionKey);
                
                if (sessionData != null && !sessionData.isEmpty()) {
                    Map<String, Object> userStatus = new HashMap<>();
                    userStatus.put("status", sessionData.getOrDefault("status", "online"));
                    if (sessionData.containsKey("last_seen")) {
                        userStatus.put("lastSeen", sessionData.get("last_seen"));
                    }
                    statuses.put(uid, userStatus);
                } else {
                    statuses.put(uid, Map.of("status", "online"));
                }
            } else {
                statuses.put(uid, Map.of("status", "offline"));
            }
        }
        
        return statuses;
    }
}
