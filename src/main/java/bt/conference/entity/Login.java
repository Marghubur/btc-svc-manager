package bt.conference.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fierhub.database.annotations.Column;
import com.fierhub.database.annotations.Id;
import com.fierhub.database.annotations.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "login")
@Builder
public class Login {

    @Id
    @Column(name = "loginId")
    private Long loginId;

    @Column(name = "userId")
    private Long userId;

    @Column(name = "email")
    private String email;

    @Column(name = "mobile")
    private String mobile;

    @Column(name = "password")
    private String password;

    @Column(name = "deviceId")
    private String deviceId;

    @Column(name = "roleId")
    private Integer roleId;

    @Column(name = "isAccountConfig")
    private Boolean isAccountConfig;

    @Column(name = "isActive")
    private Boolean isActive;

    @Column(name = "refreshToken")
    private String refreshToken;

    @Column(name = "code")
    private String code;

    @Column(name = "createdBy")
    private Long createdBy;

    @Column(name = "updatedBy")
    private Long updatedBy;

    @Column(name = "createdOn")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private Date createdOn;

    @Column(name = "updatedOn")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private Date updatedOn;
}
