package bt.conference.controller;

import bt.conference.dto.PagedResponse;
import bt.conference.entity.UserCache;
import bt.conference.model.UserCacheSearchRequest;
import bt.conference.service.UserCacheService;
import com.fierhub.model.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user-cache/")
@RequiredArgsConstructor
public class UserCacheController {

    private final UserCacheService userCacheService;

    /**
     * Get all users with pagination
     * GET /api/users?pageNumber=1&pageSize=10
     */
    @GetMapping("get-users")
    public BaseResponse getAllUsers(
            @RequestParam(defaultValue = "1") int pageNumber,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        PagedResponse<UserCache> response = userCacheService.getAllUsers(pageNumber, pageSize);
        return BaseResponse.Ok(response);
    }

    /**
     * Search users by username, email, firstName, lastName
     * GET /api/users/search?term=john&pageNumber=1&pageSize=10
     */
    @GetMapping("search")
    public BaseResponse searchUsers(
            @RequestParam(required = false) String term,
            @RequestParam(defaultValue = "1") int pageNumber,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        PagedResponse<UserCache> response = userCacheService.searchUsers(term, pageNumber, pageSize);
        return BaseResponse.Ok(response);
    }

    /**
     * Search users with advanced options (POST)
     * POST /api/users/search
     */
    @PostMapping("search")
    public BaseResponse searchUsersAdvanced(
            @RequestBody UserCacheSearchRequest request
    ) {
        PagedResponse<UserCache> response = userCacheService.searchUsers(request);
        return BaseResponse.Ok(response);
    }

    /**
     * Search only active users
     * GET /api/users/search/active?term=john&pageNumber=1&pageSize=10
     */
    @GetMapping("search/active")
    public BaseResponse searchActiveUsers(
            @RequestParam(required = false) String term,
            @RequestParam(defaultValue = "1") int pageNumber,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        PagedResponse<UserCache> response = userCacheService.searchActiveUsers(term, pageNumber, pageSize);
        return BaseResponse.Ok(response);
    }

    /**
     * Search users excluding current user (for chat)
     * GET /api/users/search/exclude/{userId}?term=john&pageNumber=1&pageSize=10
     */
    @GetMapping("search/exclude/{userId}")
    public BaseResponse searchUsersExcluding(
            @PathVariable String userId,
            @RequestParam(required = false) String term,
            @RequestParam(defaultValue = "1") int pageNumber,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        PagedResponse<UserCache> response = userCacheService.searchUsersExcluding(
                userId, term, pageNumber, pageSize
        );
        return BaseResponse.Ok(response);
    }

    /**
     * Get single user by ID
     * GET /api/users/{id}
     */
    @GetMapping("/{id}")
    public BaseResponse getUserById(@PathVariable String id) {
        return userCacheService.getUserById(id)
                .map(BaseResponse::Ok)
                .orElse(BaseResponse.RaiseError("User not found", new Exception("User not found")));
    }

    /**
     * Get user by userId
     * GET /api/users/by-user-id/{userId}
     */
    @GetMapping("/by-user-id/{userId}")
    public BaseResponse getUserByUserId(@PathVariable String userId) {
        return userCacheService.getUserByUserId(userId)
                .map(BaseResponse::Ok)
                .orElse(BaseResponse.RaiseError("User not found", new Exception("User not found")));
    }
}