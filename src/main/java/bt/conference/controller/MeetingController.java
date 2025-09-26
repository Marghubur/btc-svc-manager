package bt.conference.controller;

import bt.conference.entity.MeetingDetail;
import bt.conference.serviceinterface.IMeetingService;
import in.bottomhalf.common.models.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/meeting/")
public class MeetingController {
    @Autowired
    IMeetingService _meetingService;

    @PostMapping("generateMeeting")
    public ResponseEntity<ApiResponse> generateMeeting(@RequestBody MeetingDetail meetingDetail) throws Exception {
        var result = _meetingService.generateMeetingService(meetingDetail);
        return ResponseEntity.ok(ApiResponse.Ok(result));
    }

    @GetMapping("getAllMeetingByOrganizer")
    public ResponseEntity<ApiResponse> getAllMeetingByOrganizer() throws Exception {
        var result = _meetingService.getAllMeetingByOrganizerService();
        return ResponseEntity.ok(ApiResponse.Ok(result));
    }

    @PostMapping("generateQuickMeeting")
    public ResponseEntity<ApiResponse> generateQuickMeeting(@RequestBody MeetingDetail meetingDetail) throws Exception {
        var result = _meetingService.generateQuickMeetingService(meetingDetail);
        return ResponseEntity.ok(ApiResponse.Ok(result));
    }

    @PostMapping("validateMeeting")
    public ResponseEntity<ApiResponse> validateMeeting(@RequestBody MeetingDetail meetingDetail) throws Exception {
        var result = _meetingService.validateMeetingService(meetingDetail);
        return ResponseEntity.ok(ApiResponse.Ok(result));
    }

    @PostMapping("joinMeetingIdPassCode")
    public ResponseEntity<ApiResponse> joinMeetingIdPassCode(@RequestBody MeetingDetail meetingDetail) throws Exception {
        var result = _meetingService.validateMeetingIdPassCodeService(meetingDetail);
        return ResponseEntity.ok(ApiResponse.Ok(result));
    }
}
