package bt.conference.controller;

import bt.conference.dto.PresignedUrlRequest;
import bt.conference.dto.PresignedUrlResponse;
import bt.conference.service.StorageService;
import com.fierhub.model.BaseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(value = {"api/storage/", "api/v1/storage/"})
@RequiredArgsConstructor
public class StorageController {

    private final StorageService storageService;

    /**
     * Generate presigned URL for direct file upload to Cloudflare R2
     * POST /api/storage/presigned-url or /api/v1/storage/presigned-url
     */
    @PostMapping("presigned-url")
    public BaseResponse getPresignedUrl(@RequestBody PresignedUrlRequest request) {
        try {
            PresignedUrlResponse response = storageService.generatePresignedUploadUrl(request);
            return BaseResponse.Ok(response);
        } catch (Exception e) {
            log.error("Error creating presigned URL", e);
            return BaseResponse.RaiseError("Error creating presigned URL: " + e.getMessage(), e);
        }
    }

    @PostMapping("multipart/start")
    public BaseResponse startMultipartUpload(@RequestBody bt.conference.dto.MultipartUploadStartRequest request) {
        try {
            bt.conference.dto.MultipartUploadStartResponse response = storageService.startMultipartUpload(request);
            return BaseResponse.Ok(response);
        } catch (Exception e) {
            log.error("Error starting multipart upload", e);
            return BaseResponse.RaiseError("Error starting multipart upload: " + e.getMessage(), e);
        }
    }

    @PostMapping("multipart/url")
    public BaseResponse getMultipartPreSignedUrl(@RequestBody bt.conference.dto.MultipartUploadUrlRequest request) {
        try {
            bt.conference.dto.MultipartUploadUrlResponse response = storageService.getMultipartPreSignedUrl(request);
            return BaseResponse.Ok(response);
        } catch (Exception e) {
            log.error("Error generating multipart url", e);
            return BaseResponse.RaiseError("Error generating multipart url: " + e.getMessage(), e);
        }
    }

    @PostMapping("multipart/complete")
    public BaseResponse completeMultipartUpload(@RequestBody bt.conference.dto.MultipartUploadCompleteRequest request) {
        try {
            bt.conference.dto.MultipartUploadCompleteResponse response = storageService.completeMultipartUpload(request);
            return BaseResponse.Ok(response);
        } catch (Exception e) {
            log.error("Error completing multipart upload", e);
            return BaseResponse.RaiseError("Error completing multipart upload: " + e.getMessage(), e);
        }
    }

    @DeleteMapping("delete")
    public BaseResponse deleteFile(@RequestParam String fileKey) {
        try {
            storageService.deleteFile(fileKey);
            return BaseResponse.Ok("File deleted successfully");
        } catch (Exception e) {
            log.error("Error deleting file", e);
            return BaseResponse.RaiseError("Error deleting file: " + e.getMessage(), e);
        }
    }
}
