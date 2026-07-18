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
}
