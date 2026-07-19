package bt.conference.service;

import bt.conference.dto.*;
import bt.conference.model.ApplicationConstant;
import com.fierhub.model.UserSession;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedUploadPartRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.UploadPartPresignRequest;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

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
                    .allowedHeaders("content-type", "Content-Type", "Authorization")
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
                .contentType("application/octet-stream")
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

    public MultipartUploadStartResponse startMultipartUpload(MultipartUploadStartRequest request) {
        var filePath = request.getConversationId() + "/" + userSession.getUserId();
        filePath = filePath.replace(" ", "");
        String uniquePrefix = java.util.UUID.randomUUID().toString() + "-";
        var objectKey = filePath + "/" + uniquePrefix + request.getFileName();

        CreateMultipartUploadRequest createRequest = CreateMultipartUploadRequest.builder()
                .bucket(ApplicationConstant.FierhubBucket)
                .key(objectKey)
                .contentType(request.getContentType())
                .build();

        CreateMultipartUploadResponse response = s3Client.createMultipartUpload(createRequest);

        return MultipartUploadStartResponse.builder()
                .uploadId(response.uploadId())
                .fileKey(objectKey)
                .build();
    }

    public MultipartUploadUrlResponse getMultipartPreSignedUrl(MultipartUploadUrlRequest request) {
        UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                .bucket(ApplicationConstant.FierhubBucket)
                .key(request.getFileKey())
                .uploadId(request.getUploadId())
                .partNumber(request.getPartNumber())
                .build();

        UploadPartPresignRequest presignRequest = UploadPartPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(ApplicationConstant.SignatureDurationInMinute))
                .uploadPartRequest(uploadPartRequest)
                .build();

        PresignedUploadPartRequest presignedRequest = s3Presigner.presignUploadPart(presignRequest);

        return MultipartUploadUrlResponse.builder()
                .uploadUrl(presignedRequest.url().toString())
                .build();
    }

    public MultipartUploadCompleteResponse completeMultipartUpload(MultipartUploadCompleteRequest request) {
        List<CompletedPart> completedParts = request.getParts().stream()
                .map(part -> CompletedPart.builder()
                        .partNumber(part.getPartNumber())
                        .eTag(part.getETag())
                        .build())
                .collect(Collectors.toList());

        CompletedMultipartUpload completedMultipartUpload = CompletedMultipartUpload.builder()
                .parts(completedParts)
                .build();

        CompleteMultipartUploadRequest completeRequest = CompleteMultipartUploadRequest.builder()
                .bucket(ApplicationConstant.FierhubBucket)
                .key(request.getFileKey())
                .uploadId(request.getUploadId())
                .multipartUpload(completedMultipartUpload)
                .build();

        s3Client.completeMultipartUpload(completeRequest);

        return MultipartUploadCompleteResponse.builder()
                .publicUrl(ApplicationConstant.ResourcePublicURL + request.getFileKey())
                .build();
    }

    public void deleteFile(String fileKey) {
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(ApplicationConstant.FierhubBucket)
                .key(fileKey)
                .build();
        s3Client.deleteObject(deleteObjectRequest);
    }
}
