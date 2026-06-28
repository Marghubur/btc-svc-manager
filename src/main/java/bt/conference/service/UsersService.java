package bt.conference.service;

import bt.conference.dto.*;
import bt.conference.entity.Users;
import bt.conference.model.UsersSearchRequest;
import bt.conference.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsersService {

    private final UsersRepository usersRepository;
    private final MongoTemplate mongoTemplate;

    /**
     * Get all users with pagination
     */
    public PagedResponse<Users> getAllUsers(int pageNumber, int pageSize) {

        Pageable pageable = PageRequest.of(
                pageNumber - 1,
                pageSize,
                Sort.by(Sort.Direction.ASC, "username")
        );

        Page<Users> page = usersRepository.findAll(pageable);

        return buildResponse(page, pageNumber, pageSize);
    }

    /**
     * Search users by username, email, firstName, lastName
     */
    public PagedResponse<Users> searchUsers(UsersSearchRequest request) {

        String searchTerm = request.getSearchTerm();
        int pageNumber = request.getPageNumber();
        int pageSize = request.getPageSize();
        int skip = (pageNumber - 1) * pageSize;

        // Build sort
        Sort.Direction direction = "DESC".equalsIgnoreCase(request.getSortDirection())
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        Sort sort = Sort.by(direction, request.getSortBy());

        Query query = new Query();

        // Add search criteria if term provided
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            String pattern = searchTerm.trim();

            Criteria searchCriteria = new Criteria().orOperator(
                    Criteria.where("username").regex(pattern, "i"),
                    Criteria.where("email").regex(pattern, "i"),
                    Criteria.where("firstName").regex(pattern, "i"),
                    Criteria.where("lastName").regex(pattern, "i")
            );

            query.addCriteria(searchCriteria);
        }

        // Count total
        long totalRecords = mongoTemplate.count(query, Users.class);

        log.info("Search term: '{}', Total records: {}", searchTerm, totalRecords);

        // Add sorting and pagination
        query.with(sort);
        query.skip(skip);
        query.limit(pageSize);

        // Execute
        List<Users> users = mongoTemplate.find(query, Users.class);

        int totalPages = (int) Math.ceil((double) totalRecords / pageSize);

        return PagedResponse.of(
                users,
                totalPages,
                pageNumber,
                pageSize
        );
    }

    /**
     * Search users with simple parameters
     */
    public PagedResponse<Users> searchUsers(
            String searchTerm,
            int pageNumber,
            int pageSize
    ) {
        UsersSearchRequest request = new UsersSearchRequest();
        request.setSearchTerm(searchTerm);
        request.setPageNumber(pageNumber);
        request.setPageSize(pageSize);

        return searchUsers(request);
    }

    /**
     * Search only active users
     */
    public PagedResponse<Users> searchActiveUsers(
            String searchTerm,
            int pageNumber,
            int pageSize
    ) {
        int skip = (pageNumber - 1) * pageSize;

        Query query = new Query();

        // Only active users
        query.addCriteria(Criteria.where("status").is("ACTIVE"));

        // Add search criteria
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            String pattern = searchTerm.trim();

            Criteria searchCriteria = new Criteria().orOperator(
                    Criteria.where("username").regex(pattern, "i"),
                    Criteria.where("email").regex(pattern, "i"),
                    Criteria.where("firstName").regex(pattern, "i"),
                    Criteria.where("lastName").regex(pattern, "i")
            );

            query.addCriteria(searchCriteria);
        }

        long totalRecords = mongoTemplate.count(query, Users.class);

        query.with(Sort.by(Sort.Direction.ASC, "username"));
        query.skip(skip);
        query.limit(pageSize);

        List<Users> users = mongoTemplate.find(query, Users.class);

        int totalPages = (int) Math.ceil((double) totalRecords / pageSize);

        return PagedResponse.of(
                users,
                totalPages,
                pageNumber,
                pageSize
        );
    }

    /**
     * Search users excluding a specific user (useful for chat)
     */
    public PagedResponse<Users> searchUsersExcluding(
            String excludeUserId,
            String searchTerm,
            int pageNumber,
            int pageSize
    ) {
        int skip = (pageNumber - 1) * pageSize;

        Query query = new Query();

        // Exclude specific user
        query.addCriteria(Criteria.where("id").ne(excludeUserId));

        // Only active users
        query.addCriteria(Criteria.where("status").is("ACTIVE"));

        // Add search criteria
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            String pattern = searchTerm.trim();

            Criteria searchCriteria = new Criteria().orOperator(
                    Criteria.where("username").regex(pattern, "i"),
                    Criteria.where("email").regex(pattern, "i"),
                    Criteria.where("firstName").regex(pattern, "i"),
                    Criteria.where("lastName").regex(pattern, "i")
            );

            query.addCriteria(searchCriteria);
        }

        long totalRecords = mongoTemplate.count(query, Users.class);

        query.with(Sort.by(Sort.Direction.ASC, "username"));
        query.skip(skip);
        query.limit(pageSize);

        List<Users> users = mongoTemplate.find(query, Users.class);

        int totalPages = (int) Math.ceil((double) totalRecords / pageSize);

        return PagedResponse.of(
                users,
                totalPages,
                pageNumber,
                pageSize
        );
    }

    /**
     * Get user by ID
     */
    public Optional<Users> getUserById(String id) {
        return usersRepository.findById(id);
    }

    /**
     * Get user by userId
     */
    public Optional<Users> getUserByUserId(String odUserId) {
        return usersRepository.findById(odUserId);
    }

    /**
     * Get user by email
     */
    public Optional<Users> getUserByEmail(String email) {
        return usersRepository.findByEmail(email);
    }

    /**
     * Helper method to build PagedResponse from Page
     */
    private PagedResponse<Users> buildResponse(Page<Users> page, int pageNumber, int pageSize) {
        return PagedResponse.of(
                page.getContent(),
                page.getTotalPages(),
                pageNumber,
                pageSize
        );
    }
}
