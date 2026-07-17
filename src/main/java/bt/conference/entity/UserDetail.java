package bt.conference.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fierhub.database.annotations.Column;
import com.fierhub.database.annotations.Id;
import com.fierhub.database.annotations.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class UserDetail {

    @Id
    @Column(name = "userId")
    @JsonProperty("userId")
    private long userId;

    @Column(name = "firstName")
    @JsonProperty("firstName")
    private String firstName;

    @Column(name = "lastName")
    @JsonProperty("lastName")
    private String lastName;

    @Column(name = "fatherName")
    @JsonProperty("fatherName")
    private String fatherName;

    @Column(name = "motherName")
    @JsonProperty("motherName")
    private String motherName;

    @Column(name = "email")
    @JsonProperty("email")
    private String email;

    @Column(name = "mobile")
    @JsonProperty("mobile")
    private String mobile;

    @Column(name = "alternateNumber")
    @JsonProperty("alternateNumber")
    private String alternateNumber;

    @Column(name = "address")
    @JsonProperty("address")
    private String address;

    @Column(name = "city")
    @JsonProperty("city")
    private String city;

    @Column(name = "pinCode")
    @JsonProperty("pinCode")
    private int pinCode;

    @Column(name = "state")
    @JsonProperty("state")
    private String state;

    @Column(name = "country")
    @JsonProperty("country")
    private String country;

    @Column(name = "roleId")
    @JsonProperty("roleId")
    private int roleId;

    @Column(name = "isActive")
    @JsonProperty("isActive")
    private boolean isActive;

    @Column(name = "imageURL")
    @JsonProperty("imageURL")
    private String imageURL;

    @Column(name = "createdBy")
    @JsonProperty("createdBy")
    private long createdBy;

    @Column(name = "updatedBy")
    @JsonProperty("updatedBy")
    private long updatedBy;

    @Column(name = "createdOn")
    @JsonProperty("createdOn")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private Date createdOn;

    @Column(name = "updatedOn")
    @JsonProperty("updatedOn")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private Date updatedOn;

    @Column(name = "dateOfBirth")
    @JsonProperty("dateOfBirth")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private Date dateOfBirth;

    @Column(name = "gender")
    @JsonProperty("gender")
    private char gender;

    @Column(name = "maritalStatus")
    @JsonProperty("maritalStatus")
    private boolean maritalStatus;

    @Column(name = "religionId")
    @JsonProperty("religionId")
    private int religionId;

    @Column(name = "nationality")
    @JsonProperty("nationality")
    private String nationality;
}
