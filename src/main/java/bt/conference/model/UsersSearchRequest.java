package bt.conference.model;

import lombok.Data;

@Data
public class UsersSearchRequest {
    private String searchTerm;
    private int pageNumber = 1;
    private int pageSize = 10;
    private String sortBy = "username";
    private String sortDirection = "ASC";
}
