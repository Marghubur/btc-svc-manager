package bt.conference.serviceinterface;

import bt.conference.dto.RedisAnalysisResponse;

public interface IRedisAnalysisService {
    /**
     * Checks if Redis is accessible and responding to ping.
     *
     * @return true if Redis is available, false otherwise.
     */
    boolean isRedisAvailable();

    /**
     * Generates a Redis analysis response including presence summary,
     * paginated presence users, and Redis server info.
     *
     * @param page     1-based page number
     * @param pageSize number of records per page
     * @return RedisAnalysisResponse matching the requested TypeScript interface
     * @throws Exception if analysis fails
     */
    RedisAnalysisResponse getAnalysisReport(int page, int pageSize) throws Exception;
}
