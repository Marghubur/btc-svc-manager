package bt.conference.config;

import bt.conference.model.FileServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

@Configuration
public class FileServerConfig {
    private final FileServer fileServer;

    public FileServerConfig(FileServer fileServer) {
        this.fileServer = fileServer;
    }

    @Bean
    public S3Client s3Client() {
        AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(
                fileServer.getAccess_key_id(),
                fileServer.getSecret_access_key()
        );

        if(!fileServer.isS3Enabled()) {
            S3Configuration s3Config = S3Configuration.builder()
                    .pathStyleAccessEnabled(true)
                    .chunkedEncodingEnabled(false)
                    .build();

            return S3Client.builder()
                    .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                    .endpointOverride(URI.create(fileServer.getEndpoint()))
                    .region(Region.of(fileServer.getRegion()))
                    .serviceConfiguration(s3Config)
                    .build();
        }

        return S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .region(Region.of(fileServer.getRegion()))
                .build();
    }

    @Bean
    public software.amazon.awssdk.services.s3.presigner.S3Presigner s3Presigner() {
        AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(
                fileServer.getAccess_key_id(),
                fileServer.getSecret_access_key()
        );

        if(!fileServer.isS3Enabled()) {
            S3Configuration s3Config = S3Configuration.builder()
                    .pathStyleAccessEnabled(true)
                    .chunkedEncodingEnabled(false)
                    .build();

            return software.amazon.awssdk.services.s3.presigner.S3Presigner.builder()
                    .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                    .endpointOverride(URI.create(fileServer.getEndpoint()))
                    .region(Region.of(fileServer.getRegion()))
                    .serviceConfiguration(s3Config)
                    .build();
        }

        return software.amazon.awssdk.services.s3.presigner.S3Presigner.builder()
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .region(Region.of(fileServer.getRegion()))
                .build();
    }
}
