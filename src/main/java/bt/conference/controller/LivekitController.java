package bt.conference.controller;

import java.util.Map;

import bt.conference.model.ParticipantTokenRequest;
import bt.conference.serviceinterface.ILivekitService;
import com.fierhub.model.ApiAuthResponse;
import com.fierhub.model.ApiErrorResponse;
import com.fierhub.model.BaseResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/conference/")
public class LivekitController {
    @Autowired
    ILivekitService livekitService;

    /**
     * @return JSON object with the JWT token
     */
    @GetMapping(value = "check")
    public BaseResponse checkHealth() {
        return BaseResponse.Ok("Working");
    }

    /**
     * @param request object with roomName and participantName
     * @return JSON object with the JWT token
     */
    @PostMapping(value = "token")
    public BaseResponse createToken(@RequestBody ParticipantTokenRequest request) throws Exception {
        if (request.getRoomName() == null || request.getParticipantName() == null) {
            return ApiErrorResponse.RaiseError(
                    Map.of("errorMessage", "roomName and participantName are required"),
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ""
            );
        }

        var token = livekitService.createToken(request);
        return ApiAuthResponse.Ok(null, token);
    }
}
