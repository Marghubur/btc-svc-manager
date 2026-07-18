package bt.conference.service;

import bt.conference.dto.PresignedUrlRequest;
import bt.conference.dto.PresignedUrlResponse;
import bt.conference.model.ApplicationConstant;
import com.fierhub.model.UserSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import jakarta.annotation.PostConstruct;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CORSRule;
import software.amazon.awssdk.services.s3.model.CORSConfiguration;
import software.amazon.awssdk.services.s3.model.PutBucketCorsRequest;

@Slf4j
@Service
public class StorageService {
    @Autowired
    S3Presigner s3Presigner;
    @Autowired
    S3Client s3Client;
    @Autowired
    UserSession userSession;

    @PostConstruct
    public void initCors() {
        try {
            CORSRule rule = CORSRule.builder()
                    .allowedMethods("GET", "PUT", "POST", "DELETE", "HEAD")
                    .allowedOrigins("*")
                    .allowedHeaders("*")
                    .exposeHeaders("ETag")
                    .maxAgeSeconds(3000)
                    .build();

            CORSConfiguration configuration = CORSConfiguration.builder()
                    .corsRules(rule)
                    .build();

            s3Client.putBucketCors(PutBucketCorsRequest.builder()
                    .bucket(ApplicationConstant.FierhubBucket)
                    .corsConfiguration(configuration)
                    .build());
            log.info("Successfully configured CORS for bucket: {}", ApplicationConstant.FierhubBucket);
        } catch (Exception e) {
            log.error("Failed to configure CORS for bucket: " + ApplicationConstant.FierhubBucket, e);
        }
    }

    public PresignedUrlResponse generatePresignedUploadUrl(PresignedUrlRequest request) {
        var filePath = request.getConversationId() + "/" + userSession.getUserId();
        filePath = filePath.replace(" ", "");
        String uniquePrefix = java.util.UUID.randomUUID().toString() + "-";
        var objectKey = filePath + "/" + uniquePrefix + request.getFileName();
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(ApplicationConstant.FierhubBucket)
                .key(objectKey)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(ApplicationConstant.SignatureDurationInMinute))
                .putObjectRequest(objectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);

        return PresignedUrlResponse.builder()
                .uploadUrl(presignedRequest.url().toString())
                .fileKey(objectKey)
                .publicUrl(ApplicationConstant.ResourcePublicURL + objectKey)
                .build();
    }
}
