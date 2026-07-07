package bt.conference.repository;

import bt.conference.entity.Conversation;
import bt.conference.entity.Users;
import bt.conference.model.SearchResultItem;
import bt.conference.service.GlobalSearchException;
import bt.conference.service.GlobalSearchException.ErrorType;
import com.fierhub.model.UserSession;
import com.mongodb.MongoException;
import com.mongodb.MongoTimeoutException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Repository
public class GlobalSearchRepository {

    private static final Logger logger = LoggerFactory.getLogger(GlobalSearchRepository.class);

    private final UserSession userSession;
    private final MongoTemplate mongoTemplate;

    // Thread pool configuration
    @Value("${search.thread.core-pool-size:4}")
    private int corePoolSize;

    @Value("${search.thread.max-pool-size:16}")
    private int maxPoolSize;

    @Value("${search.thread.queue-capacity:200}")
    private int queueCapacity;

    @Value("${search.thread.keep-alive-seconds:60}")
    private long keepAliveSeconds;

    @Value("${search.timeout-seconds:5}")
    private int searchTimeoutSeconds;

    @Value("${search.typeahead.timeout-ms:1000}")
    private int typeaheadTimeoutMs;

    @Value("${search.retry.max-attempts:2}")
    private int maxRetryAttempts;

    @Value("${search.retry.delay-ms:50}")
    private long retryDelayMs;

    @Value("${search.cache.enabled:true}")
    private boolean cacheEnabled;

    @Value("${search.cache.ttl-seconds:30}")
    private int cacheTtlSeconds;

    @Value("${search.use-text-index:true}")
    private boolean useTextIndex;

    // Thread pool and state
    private volatile ThreadPoolExecutor searchExecutor;
    private final AtomicBoolean isShuttingDown = new AtomicBoolean(false);
    private final AtomicBoolean isInitialized = new AtomicBoolean(false);

    // Simple in-memory cache (consider Redis for production)
    private final ConcurrentHashMap<String, CacheEntry> searchCache = new ConcurrentHashMap<>();

    // Metrics
    private final AtomicLong totalSearches = new AtomicLong(0);
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong failedSearches = new AtomicLong(0);
    private final AtomicInteger activeSearches = new AtomicInteger(0);

    // Circuit breaker
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicLong lastFailureTime = new AtomicLong(0);
    private static final int CIRCUIT_BREAKER_THRESHOLD = 5;
    private static final long CIRCUIT_BREAKER_RESET_MS = 30000;

    // Rate limiter (simple per-user)
    private final ConcurrentHashMap<String, RateLimitEntry> rateLimiter = new ConcurrentHashMap<>();
    private static final int RATE_LIMIT_REQUESTS = 30; // requests per window
    private static final long RATE_LIMIT_WINDOW_MS = 10000; // 10 seconds

    @Autowired
    public GlobalSearchRepository(UserSession userSession, MongoTemplate mongoTemplate) {
        this.userSession = userSession;
        this.mongoTemplate = mongoTemplate;
    }

    // ==================== Initialization & Shutdown ====================

    @PostConstruct
    public synchronized void init() {
        if (isInitialized.get()) {
            return;
        }

        try {
            logger.info("Initializing GlobalSearchRepository...");

            ThreadFactory threadFactory = r -> {
                Thread t = new Thread(r, "global-search-" + System.nanoTime());
                t.setDaemon(true);
                t.setUncaughtExceptionHandler((thread, ex) -> {
                    logger.error("Uncaught exception in {}: {}", thread.getName(), ex.getMessage(), ex);
                    consecutiveFailures.incrementAndGet();
                    lastFailureTime.set(System.currentTimeMillis());
                });
                return t;
            };

            RejectedExecutionHandler rejectionHandler = (runnable, executor) -> {
                logger.warn("Task rejected - pool exhausted. Queue: {}, Active: {}",
                        executor.getQueue().size(), executor.getActiveCount());
                try {
                    if (!executor.getQueue().offer(runnable, 500, TimeUnit.MILLISECONDS)) {
                        throw new RejectedExecutionException("Queue full");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RejectedExecutionException("Interrupted", e);
                }
            };

            searchExecutor = new ThreadPoolExecutor(
                    corePoolSize, maxPoolSize,
                    keepAliveSeconds, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(queueCapacity),
                    threadFactory, rejectionHandler);
            searchExecutor.allowCoreThreadTimeOut(true);
            searchExecutor.prestartAllCoreThreads();

            isInitialized.set(true);
            logger.info("GlobalSearchRepository initialized with {} core threads", corePoolSize);

            // Log index recommendations
            logIndexRecommendations();

        } catch (Exception e) {
            logger.error("Failed to initialize GlobalSearchRepository", e);
            throw new RuntimeException("Failed to initialize search", e);
        }
    }

    @PreDestroy
    public synchronized void shutdown() {
        if (!isInitialized.get() || isShuttingDown.getAndSet(true)) {
            return;
        }

        logger.info("Shutting down GlobalSearchRepository...");

        if (searchExecutor != null) {
            searchExecutor.shutdown();
            try {
                if (!searchExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    List<Runnable> pending = searchExecutor.shutdownNow();
                    logger.warn("Force shutdown, cancelled {} tasks", pending.size());
                    searchExecutor.awaitTermination(5, TimeUnit.SECONDS);
                }
            } catch (InterruptedException e) {
                searchExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        searchCache.clear();
        logger.info("GlobalSearchRepository shutdown complete. Stats: searches={}, cacheHits={}, failures={}",
                totalSearches.get(), cacheHits.get(), failedSearches.get());
    }

    private void logIndexRecommendations() {
        logger.info("=== MongoDB Index Recommendations for Optimal Search Performance ===");
        logger.info("Run these commands in MongoDB shell:");
        logger.info("");
        logger.info("// Text indexes for full-text search (RECOMMENDED)");
        logger.info("db.users.createIndex({");
        logger.info("  firstName: 'text', lastName: 'text', email: 'text', username: 'text'");
        logger.info(
                "}, { name: 'user_text_search', weights: { firstName: 10, lastName: 10, username: 5, email: 3 }});");
        logger.info("");
        logger.info("db.conversation.createIndex({");
        logger.info("  conversation_name: 'text', 'participants.username': 'text', 'participants.email': 'text'");
        logger.info("}, { name: 'conversation_text_search' });");
        logger.info("");
        logger.info("// Compound indexes for filtering");
        logger.info("db.users.createIndex({ status: 1, updatedAt: -1 });");
        logger.info("db.conversation.createIndex({ is_active: 1, 'participant_ids': 1, updated_at: -1 });");
        logger.info("================================================================");
    }

    // ==================== Health & Metrics ====================

    public boolean isHealthy() {
        return isInitialized.get() && !isShuttingDown.get()
                && searchExecutor != null && !searchExecutor.isShutdown()
                && !isCircuitBreakerOpen();
    }

    private boolean isCircuitBreakerOpen() {
        if (consecutiveFailures.get() >= CIRCUIT_BREAKER_THRESHOLD) {
            if (System.currentTimeMillis() - lastFailureTime.get() < CIRCUIT_BREAKER_RESET_MS) {
                return true;
            }
            consecutiveFailures.set(0);
        }
        return false;
    }

    public Map<String, Object> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalSearches", totalSearches.get());
        metrics.put("cacheHits", cacheHits.get());
        metrics.put("cacheHitRate", totalSearches.get() > 0
                ? (double) cacheHits.get() / totalSearches.get() * 100
                : 0);
        metrics.put("failedSearches", failedSearches.get());
        metrics.put("activeSearches", activeSearches.get());
        metrics.put("cacheSize", searchCache.size());
        metrics.put("isHealthy", isHealthy());

        if (searchExecutor != null) {
            metrics.put("poolSize", searchExecutor.getPoolSize());
            metrics.put("activeThreads", searchExecutor.getActiveCount());
            metrics.put("queueSize", searchExecutor.getQueue().size());
        }
        return metrics;
    }

    // ==================== Rate Limiting ====================

    private void checkRateLimit(String userId) {
        if (userId == null)
            return;

        rateLimiter.compute(userId, (key, entry) -> {
            long now = System.currentTimeMillis();
            if (entry == null || now - entry.windowStart > RATE_LIMIT_WINDOW_MS) {
                return new RateLimitEntry(now, 1);
            }
            if (entry.count >= RATE_LIMIT_REQUESTS) {
                throw new GlobalSearchException(ErrorType.RATE_LIMITED, "");
            }
            entry.count++;
            return entry;
        });

        // Cleanup old entries periodically
        if (rateLimiter.size() > 10000) {
            long now = System.currentTimeMillis();
            rateLimiter.entrySet().removeIf(e -> now - e.getValue().windowStart > RATE_LIMIT_WINDOW_MS * 2);
        }
    }

    // ==================== Caching ====================

    private String buildCacheKey(String searchTerm, String userId, boolean isTypeahead, int limit) {
        return String.format("%s:%s:%s:%d", searchTerm.toLowerCase(), userId, isTypeahead, limit);
    }

    private SearchResultItem getFromCache(String cacheKey) {
        if (!cacheEnabled)
            return null;

        CacheEntry entry = searchCache.get(cacheKey);
        if (entry != null && !entry.isExpired(cacheTtlSeconds)) {
            cacheHits.incrementAndGet();
            return entry.results;
        }
        searchCache.remove(cacheKey);
        return null;
    }

    private void putInCache(String cacheKey, SearchResultItem results) {
        if (!cacheEnabled)
            return;

        searchCache.put(cacheKey, new CacheEntry(results));

        // Limit cache size
        if (searchCache.size() > 5000) {
            // Remove oldest 20%
            searchCache.entrySet().stream()
                    .sorted(Comparator.comparingLong(e -> e.getValue().createdAt))
                    .limit(1000)
                    .map(Map.Entry::getKey)
                    .forEach(searchCache::remove);
        }
    }

    // ==================== Main Search Methods ====================

    /**
     * Typeahead search - fast, limited results, prefix matching
     * Used as user types in search box
     */
    public SearchResultItem typeaheadSearch(String searchTerm, String userId, String fullSearch, int limit) {
        validateAndPrepare(searchTerm, userId);

        String cacheKey = buildCacheKey(searchTerm, userId, true, limit);
        SearchResultItem cached = getFromCache(cacheKey);
        if (cached != null) {
            return cached;
        }

        try {
            SearchResultItem results = executeParallelSearch(searchTerm, userId, 0, limit, fullSearch, true);
            putInCache(cacheKey, results);
            return results;
        } finally {
            activeSearches.decrementAndGet();
        }
    }

    /**
     * Full global search with pagination
     * Used when user presses Enter or clicks "See all results"
     */
    public SearchResultItem globalSearch(
            String searchTerm, String userId, int skip, int limit) {

        validateAndPrepare(searchTerm, userSession.getUserId());

        String cacheKey = buildCacheKey(searchTerm + ":" + skip, userSession.getUserId(), false, limit);
        SearchResultItem cached = getFromCache(cacheKey);
        if (cached != null) {
            return cached;
        }

        try {
            SearchResultItem results = executeParallelSearch(
                    searchTerm, userId != null ? userId : userSession.getUserId(), skip, limit, "y", false);
            putInCache(cacheKey, results);
            return results;
        } finally {
            activeSearches.decrementAndGet();
        }
    }

    /**
     * Optimized search only in users documents based on firstName and lastName
     * with pagination, sortBy, pageIndex, and pageSize.
     */
    public SearchResultItem globalSearch(
            String searchTerm, int pageIndex, int pageSize, String sortBy, String sortDirection) {

        validateAndPrepare(searchTerm, userSession.getUserId());

        // Cache key specific to user search with pagination/sorting parameters
        String cacheKey = String.format("users-only:%s:%d:%d:%s:%s",
                searchTerm.toLowerCase(), pageIndex, pageSize,
                sortBy != null ? sortBy.toLowerCase() : "default",
                sortDirection != null ? sortDirection.toLowerCase() : "default");

        SearchResultItem cached = getFromCache(cacheKey);
        if (cached != null) {
            return cached;
        }

        try {
            SearchResultItem results = executeParallelSearch(
                    searchTerm, pageIndex, pageSize, sortBy, sortDirection);
            putInCache(cacheKey, results);
            return results;
        } finally {
            activeSearches.decrementAndGet();
        }
    }

    private void validateAndPrepare(String searchTerm, String userId) {
        if (searchTerm == null || searchTerm.trim().length() < 2) {
            throw new GlobalSearchException(ErrorType.INVALID_INPUT, searchTerm);
        }
        if (!isInitialized.get() || isShuttingDown.get()) {
            throw new GlobalSearchException(ErrorType.THREAD_POOL_SHUTDOWN, searchTerm);
        }
        if (isCircuitBreakerOpen()) {
            throw new GlobalSearchException(ErrorType.THREAD_POOL_EXHAUSTED, searchTerm);
        }

        checkRateLimit(userId);
        totalSearches.incrementAndGet();
        activeSearches.incrementAndGet();
    }

    // ==================== Parallel Search Execution ====================
    private SearchResultItem executeParallelSearch(
            String searchTerm, String userId, int skip, int limit, String fullSearch, boolean isTypeAhead) {

        int timeoutMs = isTypeAhead ? typeaheadTimeoutMs : searchTimeoutSeconds * 1000;

        try {
            // Launch parallel searches
            CompletableFuture<List<Users>> usersFuture = CompletableFuture
                    .supplyAsync(() -> searchUsers(searchTerm, fullSearch.equals("y"), skip, limit), searchExecutor)
                    .exceptionally(ex -> {
                        logger.warn("User search failed: {}", ex.getMessage());
                        return Collections.emptyList();
                    });

            CompletableFuture<List<Conversation>> conversationsFuture = CompletableFuture
                    .supplyAsync(() -> searchConversations(searchTerm, userId, skip, limit), searchExecutor)
                    .exceptionally(ex -> {
                        logger.warn("Conversation search failed: {}", ex.getMessage());
                        return Collections.emptyList();
                    });

            CompletableFuture.allOf(usersFuture, conversationsFuture)
                    .get(timeoutMs, TimeUnit.MILLISECONDS);

            consecutiveFailures.set(0); // Reset circuit breaker
            List<Users> users = new ArrayList<>(usersFuture.join());
            List<Conversation> conversations = conversationsFuture.join();

            // Users that already have a direct conversation
            Set<String> conversationUserIds = conversations.stream()
                    .map(Conversation::getCreatedBy)
                    .collect(Collectors.toSet());

            // Remove users who already have a conversation
            users.removeIf(user -> conversationUserIds.contains(user.getId()));

            return SearchResultItem.builder()
                    .conversation(conversations)
                    .users(users)
                    .build();
        } catch (TimeoutException e) {
            logger.warn("Search timeout after {}ms for term: {}", timeoutMs, searchTerm);
            // Return partial results if available

            return SearchResultItem.builder()
                    .conversation(Collections.emptyList())
                    .users(Collections.emptyList())
                    .build();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GlobalSearchException(ErrorType.THREAD_INTERRUPTED, searchTerm, e);

        } catch (ExecutionException e) {
            recordFailure();
            throw new GlobalSearchException(ErrorType.EXECUTION_FAILED, searchTerm, e.getCause());
        }
    }

    /**
     * Highly optimized parallel search on users documents based on firstName and
     * lastName.
     * Executes the paginated data query and the total count query concurrently
     * to maximize throughput and minimize latency on millions of records.
     */
    private SearchResultItem executeParallelSearch(
            String searchTerm, int pageIndex, int pageSize, String sortBy, String sortDirection) {

        int timeoutMs = searchTimeoutSeconds * 1000;
        String term = searchTerm.trim();
        int limit = pageSize > 0 ? pageSize : 10;
        int skip = pageIndex > 0 ? pageIndex * limit : 0;

        // Build base queries
        Query dataQuery = buildUserSearchQuery(term);
        Query countQuery = buildUserSearchQuery(term);

        // Optimize performance: use projection to avoid loading unnecessary/large
        // fields
        // and reduce network payload from MongoDB.
        dataQuery.fields()
                .include("id")
                .include("firstName")
                .include("lastName")
                .include("username")
                .include("email")
                .include("avatarUrl")
                .include("status")
                .include("updatedAt");

        // Apply Sorting (ensure corresponding index exists in DB, e.g. { firstName: 1 }
        // or { updatedAt: -1 })
        String sortField = (sortBy != null && !sortBy.trim().isEmpty()) ? sortBy : "updatedAt";
        Sort.Direction direction = "DESC".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;
        dataQuery.with(Sort.by(direction, sortField));

        // Apply Pagination
        dataQuery.skip(skip).limit(limit);

        // Execute Find (data) and Count queries in parallel using CompletableFuture
        CompletableFuture<List<Users>> dataFuture = CompletableFuture.supplyAsync(() -> {
            logger.debug("Executing parallel user search data query for: {}", term);
            return mongoTemplate.find(dataQuery, Users.class, "users");
        }, searchExecutor).exceptionally(ex -> {
            logger.error("Parallel user data query failed for term: " + term, ex);
            return Collections.emptyList();
        });

        CompletableFuture<Long> countFuture = CompletableFuture.supplyAsync(() -> {
            logger.debug("Executing parallel user count query for: {}", term);
            return mongoTemplate.count(countQuery, Users.class, "users");
        }, searchExecutor).exceptionally(ex -> {
            logger.error("Parallel user count query failed for term: " + term, ex);
            return 0L;
        });

        try {
            // Wait for both futures to complete with a strict timeout
            CompletableFuture.allOf(dataFuture, countFuture)
                    .get(timeoutMs, TimeUnit.MILLISECONDS);

            List<Users> users = dataFuture.join();
            long totalCount = countFuture.join();

            consecutiveFailures.set(0); // Reset circuit breaker on success

            return SearchResultItem.builder()
                    .users(users)
                    .conversation(Collections.emptyList())
                    .metadata(SearchResultItem.SearchMetadata.builder()
                            .searchTerm(term)
                            .totalCount(totalCount)
                            .page(pageIndex)
                            .limit(limit)
                            .build())
                    .build();

        } catch (TimeoutException e) {
            logger.warn("Parallel user search timed out after {}ms for term: {}", timeoutMs, term);
            // Gracefully return empty/partial results if timed out to keep the system
            // responsive
            return SearchResultItem.builder()
                    .users(Collections.emptyList())
                    .conversation(Collections.emptyList())
                    .metadata(SearchResultItem.SearchMetadata.builder()
                            .searchTerm(term)
                            .totalCount(0)
                            .page(pageIndex)
                            .limit(limit)
                            .build())
                    .build();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GlobalSearchException(ErrorType.THREAD_INTERRUPTED, term, e);

        } catch (ExecutionException e) {
            recordFailure();
            throw new GlobalSearchException(ErrorType.EXECUTION_FAILED, term, e.getCause());
        }
    }

    private Query buildUserSearchQuery(String searchTerm) {
        Query query = new Query();
        query.addCriteria(Criteria.where("status").is("ACTIVE"));

        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            Pattern pattern = Pattern.compile(escapeRegex(searchTerm.trim()), Pattern.CASE_INSENSITIVE);
            query.addCriteria(new Criteria().orOperator(
                    Criteria.where("firstName").regex(pattern),
                    Criteria.where("lastName").regex(pattern)));
        }
        return query;
    }

    private List<SearchResultItem> getCompletedResult(CompletableFuture<List<SearchResultItem>> future) {
        try {
            if (future.isDone() && !future.isCompletedExceptionally()) {
                return future.getNow(Collections.emptyList());
            }
        } catch (Exception ignored) {
        }
        return Collections.emptyList();
    }

    private void recordFailure() {
        consecutiveFailures.incrementAndGet();
        lastFailureTime.set(System.currentTimeMillis());
        failedSearches.incrementAndGet();
    }

    // ==================== User Search ====================

    private List<Users> searchUsers(String searchTerm, boolean required, int skip, int limit) {
        try {
            Query query = new Query();
            query.addCriteria(Criteria.where("status").is("ACTIVE"));

            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                Pattern pattern = Pattern.compile(escapeRegex(searchTerm.trim()), Pattern.CASE_INSENSITIVE);
                query.addCriteria(new Criteria().orOperator(
                        Criteria.where("firstName").regex(pattern),
                        Criteria.where("lastName").regex(pattern),
                        Criteria.where("email").regex(pattern),
                        Criteria.where("username").regex(pattern)));
            }

            query.skip(skip).limit(limit);
            query.with(Sort.by(Sort.Direction.DESC, "updatedAt"));

            return mongoTemplate.find(query, Users.class, "users");
        } catch (MongoTimeoutException e) {
            throw new GlobalSearchException(ErrorType.TIMEOUT, searchTerm, e);
        } catch (MongoException e) {
            throw new GlobalSearchException(ErrorType.DATABASE_ERROR, searchTerm, e);
        }
    }

    private Users mapToUserResult(Document doc, String searchTerm) {
        String firstName = doc.getString("firstName");
        String lastName = doc.getString("lastName");
        String fullName = (firstName != null ? firstName : "") +
                (lastName != null ? " " + lastName : "");

        // Calculate simple relevance score
        double score = calculateRelevanceScore(searchTerm,
                firstName, lastName, doc.getString("email"), doc.getString("username"));

        // Build highlight map
        Map<String, String> highlights = buildHighlights(searchTerm,
                Map.of("name", fullName,
                        "email", doc.getString("email") != null ? doc.getString("email") : "",
                        "username", doc.getString("username") != null ? doc.getString("username") : ""));

        return Users.builder()
                // .type(SearchResultItem.ResultType.USER)
                // .id(doc.getString("id"))
                // .title(fullName.trim())
                // .subtitle(doc.getString("email"))
                // .avatar(doc.getString("avatarUrl"))
                // .status(doc.getString("status"))
                // .score(score)
                // .highlights(highlights)
                // .lastActivity(doc.getDate("updatedAt") != null
                // ? doc.getDate("updatedAt").toInstant() : null)
                // .metadata(Map.of("username", doc.getString("username") != null ?
                // doc.getString("username") : ""))
                .build();
    }

    // ==================== Conversation Search ====================

    private List<Conversation> searchConversations(String searchTerm, String userId, int skip, int limit) {
        try {
            Query query = new Query();
                // 1. Fetch conversation IDs where the user is explicitly enrolled in
                // ConversationMembers
                query.addCriteria(
                        new Criteria().andOperator(
                                Criteria.where("isDeleted").is(false),
                                Criteria.where("searchableMemberInfo")
                                        .regex(Pattern.quote(searchTerm), "i")
                        )
                );
            //query.skip(skip).limit(limit);
//            query.with(Sort.by(Sort.Direction.DESC, "lastMessageAt"));

                var result =  mongoTemplate.find(query, Conversation.class);
                return result;
        } catch (MongoTimeoutException e) {
            throw new GlobalSearchException(ErrorType.TIMEOUT, searchTerm, e);
        } catch (MongoException e) {
            throw new GlobalSearchException(ErrorType.DATABASE_ERROR, searchTerm, e);
        }
    }

    // private SearchResultItem mapToConversationResult(Document doc, String
    // searchTerm, String currentUserId) {
    // String conversationName = doc.getString("conversation_name");
    // String conversationType = doc.getString("conversation_type");
    //
    // // For direct chats, show other participant's name
    // String displayName = conversationName;
    // String subtitle = conversationType;
    //
    // @SuppressWarnings("unchecked")
    // List<Document> participants = (List<Document>) doc.get("participants");
    // if ("direct".equals(conversationType) && participants != null &&
    // currentUserId != null) {
    // for (Document p : participants) {
    // if (!currentUserId.equals(p.getString("user_id"))) {
    // displayName = p.getString("username");
    // subtitle = p.getString("email");
    // break;
    // }
    // }
    // }
    //
    // double score = calculateRelevanceScore(searchTerm, conversationName,
    // displayName, null, null);
    //
    // return SearchResultItem.builder()
    // .type(SearchResultItem.ResultType.CONVERSATION)
    // .id(doc.getObjectId("_id").toString())
    // .title(displayName)
    // .subtitle(subtitle)
    // .score(score)
    // .lastActivity(doc.getDate("updated_at") != null
    // ? doc.getDate("updated_at").toInstant() : null)
    // .metadata(Map.of(
    // "type", conversationType != null ? conversationType : "unknown",
    // "participantCount", participants != null ? participants.size() : 0
    // ))
    // .build();
    // }

    // ==================== Relevance & Highlighting ====================

    private double calculateRelevanceScore(String searchTerm, String... fields) {
        double score = 0;
        String searchLower = searchTerm.toLowerCase();

        for (int i = 0; i < fields.length; i++) {
            String field = fields[i];
            if (field == null)
                continue;

            String fieldLower = field.toLowerCase();
            double weight = 1.0 / (i + 1); // Earlier fields have higher weight

            if (fieldLower.equals(searchLower)) {
                score += 100 * weight; // Exact match
            } else if (fieldLower.startsWith(searchLower)) {
                score += 80 * weight; // Prefix match
            } else if (fieldLower.contains(searchLower)) {
                score += 50 * weight; // Contains match
            }
        }

        return score;
    }

    private Map<String, String> buildHighlights(String searchTerm, Map<String, String> fields) {
        Map<String, String> highlights = new HashMap<>();
        String searchLower = searchTerm.toLowerCase();

        for (Map.Entry<String, String> entry : fields.entrySet()) {
            String value = entry.getValue();
            if (value != null && value.toLowerCase().contains(searchLower)) {
                // Simple highlight: wrap match in <mark> tags
                String highlighted = value.replaceAll(
                        "(?i)(" + Pattern.quote(searchTerm) + ")",
                        "<mark>$1</mark>");
                highlights.put(entry.getKey(), highlighted);
            }
        }

        return highlights.isEmpty() ? null : highlights;
    }

    private String escapeRegex(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("([\\\\\\.\\*\\+\\?\\^\\$\\{\\}\\(\\)\\|\\[\\]])", "\\\\$1");
    }

    // ==================== Helper Classes ====================

    private static class CacheEntry {
        final SearchResultItem results;
        final long createdAt;

        CacheEntry(SearchResultItem results) {
            this.results = results;
            this.createdAt = System.currentTimeMillis();
        }

        boolean isExpired(int ttlSeconds) {
            return System.currentTimeMillis() - createdAt > ttlSeconds * 1000L;
        }
    }

    private static class RateLimitEntry {
        long windowStart;
        int count;

        RateLimitEntry(long windowStart, int count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}