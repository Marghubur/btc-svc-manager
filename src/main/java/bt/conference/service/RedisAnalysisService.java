package bt.conference.service;

import bt.conference.dto.*;
import bt.conference.entity.Users;
import bt.conference.repository.UsersRepository;
import bt.conference.serviceinterface.IRedisAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
public class RedisAnalysisService implements IRedisAnalysisService {

    private final StringRedisTemplate redisTemplate;
    private final UsersRepository usersRepository;

    @Autowired
    public RedisAnalysisService(
            @Autowired(required = false) StringRedisTemplate redisTemplate,
            @Autowired(required = false) UsersRepository usersRepository
    ) {
        this.redisTemplate = redisTemplate;
        this.usersRepository = usersRepository;
    }

    @Override
    public boolean isRedisAvailable() {
        if (redisTemplate == null) {
            return false;
        }
        try {
            RedisConnectionFactory factory = redisTemplate.getConnectionFactory();
            if (factory == null) {
                return false;
            }
            try (RedisConnection connection = factory.getConnection()) {
                String pingResult = connection.ping();
                return "PONG".equalsIgnoreCase(pingResult);
            }
        } catch (Exception e) {
            log.warn("Redis health check failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public RedisAnalysisResponse getAnalysisReport(int page, int pageSize) throws Exception {
        if (!isRedisAvailable()) {
            throw new IllegalStateException("Redis is not available");
        }

        double now = Instant.now().getEpochSecond();

        // 1. Fetch Redis INFO properties
        Properties info = redisTemplate.execute((RedisCallback<Properties>) connection -> {
            try {
                return connection.serverCommands().info();
            } catch (Exception e) {
                log.warn("Failed to fetch INFO command from Redis: {}", e.getMessage());
                return new Properties();
            }
        });

        if (info == null) {
            info = new Properties();
        }

        // Total keys in database
        Long totalRedisKeys = parseLong(getProperty(info, "db0", null));
        if (totalRedisKeys == null) {
            try {
                totalRedisKeys = redisTemplate.execute((RedisCallback<Long>) RedisConnection::dbSize);
            } catch (Exception e) {
                totalRedisKeys = 0L;
            }
        }

        // 2. Build ServerInfo
        long usedMemory = parseLong(getProperty(info, "used_memory", "0"));
        long peakMemory = parseLong(getProperty(info, "used_memory_peak", "0"));
        long totalSystemMemory = parseLong(getProperty(info, "total_system_memory", "0"));

        ServerInfo serverInfo = ServerInfo.builder()
                .version(getProperty(info, "redis_version", "unknown"))
                .mode(getProperty(info, "redis_mode", "standalone"))
                .os(getProperty(info, "os", "unknown"))
                .uptimeInSeconds(parseLong(getProperty(info, "uptime_in_seconds", null)))
                .uptimeInDays(parseLong(getProperty(info, "uptime_in_days", null)))
                .connectedClients(parseInt(getProperty(info, "connected_clients", null)))
                .usedMemory(usedMemory)
                .usedMemoryHuman(getProperty(info, "used_memory_human", formatBytes(usedMemory)))
                .peakMemory(peakMemory)
                .peakMemoryHuman(getProperty(info, "used_memory_peak_human", formatBytes(peakMemory)))
                .totalSystemMemory(totalSystemMemory)
                .totalSystemMemoryHuman(getProperty(info, "total_system_memory_human", formatBytes(totalSystemMemory)))
                .memFragmentationRatio(parseDouble(getProperty(info, "mem_fragmentation_ratio", "0.0")))
                .totalKeys(totalRedisKeys)
                .instantaneousOpsPerSec(parseLong(getProperty(info, "instantaneous_ops_per_sec", null)))
                .build();

        // 3. Discover user IDs from Redis presence keys
        Set<String> discoveredUserIds = new HashSet<>();
        try {
            ScanOptions scanOptions = ScanOptions.scanOptions().match("presence:user:*:sessions").count(1000).build();
            try (Cursor<String> cursor = redisTemplate.scan(scanOptions)) {
                while (cursor.hasNext()) {
                    String key = cursor.next();
                    // key format: presence:user:{userId}:sessions
                    String[] parts = key.split(":");
                    if (parts.length >= 3) {
                        discoveredUserIds.add(parts[2]);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error scanning presence keys from Redis: {}", e.getMessage());
        }

        // Fetch user profiles from MongoDB repository if available
        Map<String, Users> userProfileMap = new HashMap<>();
        if (usersRepository != null) {
            try {
                List<Users> dbUsers = usersRepository.findAll();
                for (Users u : dbUsers) {
                    if (u.getId() != null) {
                        userProfileMap.put(u.getId(), u);
                        discoveredUserIds.add(u.getId());
                    }
                }
            } catch (Exception e) {
                log.warn("Could not fetch user profiles from database: {}", e.getMessage());
            }
        }

        // 4. Build PresenceUser list & compute PresenceSummary
        long onlineCount = 0;
        long offlineCount = 0;
        long awayCount = 0;
        long busyCount = 0;
        long totalActiveSessionsCount = 0;

        List<PresenceUser> allPresenceUsers = new ArrayList<>();

        for (String uid : discoveredUserIds) {
            String sessionsKey = "presence:user:" + uid + ":sessions";

            Set<ZSetOperations.TypedTuple<String>> activeSessionTuples = null;
            try {
                activeSessionTuples = redisTemplate.opsForZSet().rangeByScoreWithScores(sessionsKey, now, Double.MAX_VALUE);
            } catch (Exception e) {
                log.debug("Error fetching sessions for user {}: {}", uid, e.getMessage());
            }

            List<PresenceSession> sessionList = new ArrayList<>();
            String userEffectiveStatus = "offline";
            Object lastSeen = null;

            if (activeSessionTuples != null && !activeSessionTuples.isEmpty()) {
                totalActiveSessionsCount += activeSessionTuples.size();
                userEffectiveStatus = "online";

                for (ZSetOperations.TypedTuple<String> tuple : activeSessionTuples) {
                    String sessionId = tuple.getValue();
                    Double expiresAt = tuple.getScore();
                    String sessionKey = "presence:session:" + sessionId;

                    Map<Object, Object> sessionData = Collections.emptyMap();
                    try {
                        sessionData = redisTemplate.opsForHash().entries(sessionKey);
                    } catch (Exception e) {
                        log.debug("Error fetching session metadata for {}: {}", sessionId, e.getMessage());
                    }

                    String sStatus = sessionData.getOrDefault("status", "online").toString();
                    if ("busy".equalsIgnoreCase(sStatus) || "dnd".equalsIgnoreCase(sStatus)) {
                        userEffectiveStatus = "busy";
                    } else if ("away".equalsIgnoreCase(sStatus) && !"busy".equalsIgnoreCase(userEffectiveStatus)) {
                        userEffectiveStatus = "away";
                    }

                    if (sessionData.containsKey("last_seen")) {
                        lastSeen = sessionData.get("last_seen");
                    }

                    sessionList.add(PresenceSession.builder()
                            .sessionId(sessionId)
                            .status(sStatus)
                            .lastSeen(sessionData.get("last_seen"))
                            .client((String) sessionData.get("client"))
                            .device((String) sessionData.get("device"))
                            .expiresAt(expiresAt)
                            .build());
                }
            } else {
                userEffectiveStatus = "offline";
            }

            // Update summary counters
            switch (userEffectiveStatus.toLowerCase()) {
                case "online" -> onlineCount++;
                case "busy", "dnd" -> busyCount++;
                case "away" -> awayCount++;
                default -> offlineCount++;
            }

            Users profile = userProfileMap.get(uid);
            String username = profile != null ? profile.getUsername() : null;
            String email = profile != null ? profile.getEmail() : null;
            String firstName = profile != null ? profile.getFirstName() : null;
            String lastName = profile != null ? profile.getLastName() : null;
            String avatarUrl = profile != null ? profile.getAvatarUrl() : null;

            String displayName = null;
            if (firstName != null || lastName != null) {
                displayName = ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
            }

            allPresenceUsers.add(PresenceUser.builder()
                    .userId(uid)
                    .username(username)
                    .email(email)
                    .firstName(firstName)
                    .lastName(lastName)
                    .displayName(displayName)
                    .avatarUrl(avatarUrl)
                    .status(userEffectiveStatus)
                    .lastSeen(lastSeen)
                    .activeSessions(sessionList.size())
                    .sessions(sessionList)
                    .build());
        }

        // Sort users: online first, then alphabetical by username or userId
        allPresenceUsers.sort((u1, u2) -> {
            int s1 = "online".equalsIgnoreCase(u1.getStatus()) ? 0 : "busy".equalsIgnoreCase(u1.getStatus()) ? 1 : "away".equalsIgnoreCase(u1.getStatus()) ? 2 : 3;
            int s2 = "online".equalsIgnoreCase(u2.getStatus()) ? 0 : "busy".equalsIgnoreCase(u2.getStatus()) ? 1 : "away".equalsIgnoreCase(u2.getStatus()) ? 2 : 3;
            if (s1 != s2) {
                return Integer.compare(s1, s2);
            }
            String n1 = u1.getUsername() != null ? u1.getUsername() : u1.getUserId();
            String n2 = u2.getUsername() != null ? u2.getUsername() : u2.getUserId();
            return n1.compareToIgnoreCase(n2);
        });

        // 5. Pagination calculation
        long totalUsers = allPresenceUsers.size();
        int totalPages = totalUsers == 0 ? 1 : (int) Math.ceil((double) totalUsers / pageSize);

        int fromIndex = (page - 1) * pageSize;
        List<PresenceUser> pagedUsers = new ArrayList<>();
        if (fromIndex < allPresenceUsers.size() && fromIndex >= 0) {
            int toIndex = Math.min(fromIndex + pageSize, allPresenceUsers.size());
            pagedUsers = allPresenceUsers.subList(fromIndex, toIndex);
        }

        PaginationMeta paginationMeta = PaginationMeta.builder()
                .page(page)
                .pageSize(pageSize)
                .total(totalUsers)
                .totalPages(totalPages)
                .hasNext(page < totalPages)
                .hasPrevious(page > 1)
                .build();

        PresenceSummary summary = PresenceSummary.builder()
                .totalUsers(totalUsers)
                .online(onlineCount)
                .offline(offlineCount)
                .away(awayCount)
                .busy(busyCount)
                .activeSessions(totalActiveSessionsCount)
                .build();

        return RedisAnalysisResponse.builder()
                .timestamp(Instant.now().toString())
                .pagination(paginationMeta)
                .summary(summary)
                .users(pagedUsers)
                .serverInfo(serverInfo)
                .build();
    }

    private String getProperty(Properties props, String key, String defaultValue) {
        if (props == null || !props.containsKey(key)) {
            return defaultValue;
        }
        return props.getProperty(key, defaultValue);
    }

    private Long parseLong(String val) {
        if (val == null || val.trim().isEmpty()) return null;
        try {
            return Long.parseLong(val.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parseInt(String val) {
        if (val == null || val.trim().isEmpty()) return null;
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double parseDouble(String val) {
        if (val == null || val.trim().isEmpty()) return null;
        try {
            return Double.parseDouble(val.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format(Locale.ENGLISH, "%.2f %cB", bytes / Math.pow(1024, exp), pre);
    }
}
