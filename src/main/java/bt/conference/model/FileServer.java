package bt.conference.model;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "file-server.repo")
@Data
public class FileServer {
    private String access_key_id;
    private String secret_access_key;
    private String endpoint;
    private String bucket_name;
    private String region;
    private boolean s3Enabled;
}
