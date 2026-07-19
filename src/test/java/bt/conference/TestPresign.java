package bt.conference;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;
import java.time.Duration;

public class TestPresign {
    public static void main(String[] args) throws Exception {
        AwsBasicCredentials creds = AwsBasicCredentials.create("0c07413954dffe1fcf65b98a390b48d0", "92a2a0a25dc9da32a3536da4d528cb9cc52bf1c4cc3daeb36c1c9ab22c953ca0");
        S3Configuration s3Config = S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .chunkedEncodingEnabled(false)
                .build();
        S3Presigner presigner = S3Presigner.builder()
                .credentialsProvider(StaticCredentialsProvider.create(creds))
                .endpointOverride(URI.create("https://e6da13d502a12ac3505c03f6b6a2f683.r2.cloudflarestorage.com"))
                .region(Region.of("auto"))
                .serviceConfiguration(s3Config)
                .build();

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket("fierhub")
                .key("test/file.txt")
                .contentType("application/octet-stream")
                .build();
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .putObjectRequest(objectRequest)
                .build();
        PresignedPutObjectRequest presigned = presigner.presignPutObject(presignRequest);
        System.out.println("URL: " + presigned.url());
    }
}
