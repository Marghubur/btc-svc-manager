package bt.conference.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fierhub.database.annotations.Column;
import com.fierhub.database.annotations.Id;
import com.fierhub.database.annotations.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "login_refresh_token")
public class LoginRefreshToken {
    @Id
    @Column(name = "correlation_id")
    public String correlation_id;
    @Column(name = "refresh_token")
    public String refresh_token;
    @Column(name = "email_id")
    public String email_id;
    @Column(name = "last_generated_on")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    public Date last_generated_on;
}
