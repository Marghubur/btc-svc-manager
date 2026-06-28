package bt.conference.dto;

import bt.conference.entity.Conversation;
import bt.conference.entity.Users;
import lombok.Data;

import java.util.List;

@Data
public class SearchResponse {
    private List<Conversation> conversations;
    private List<Users> newUsers;  // Users without existing direct chat
}