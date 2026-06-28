package bt.conference.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "users")
public class Users {

    @Id
    private String id;

    @Field("avatarUrl")
    private String avatarUrl;

    @Field("createdAt")
    private Instant createdAt;

    @Field("email")
    private String email;

    @Field("firstName")
    private String firstName;

    @Field("lastName")
    private String lastName;

    @Field("status")
    private String status;

    @Field("updatedAt")
    private Instant updatedAt;

    @Field("username")
    private String username;
}
