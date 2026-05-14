package bt.conference.controller;

import bt.conference.entity.MeetingDetail;
import bt.conference.serviceinterface.IMeetingService;
import com.fierhub.model.BaseResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/meeting/")
public class MeetingController {
    @Autowired
    IMeetingService _meetingService;

    @PostMapping("generateMeeting")
    public BaseResponse generateMeeting(@RequestBody MeetingDetail meetingDetail) throws Exception {
        var result = _meetingService.generateMeetingService(meetingDetail);
        return BaseResponse.Ok(result);
    }

    @GetMapping("get-recent-meetings")
    public BaseResponse getRecentMeetings() throws Exception {
        var result = _meetingService.getRecentMeetingsService();
        return BaseResponse.Ok(result);
    }

    @PostMapping("generateQuickMeeting")
    public BaseResponse generateQuickMeeting(@RequestBody MeetingDetail meetingDetail) throws Exception {
        var result = _meetingService.generateQuickMeetingService(meetingDetail);
        return BaseResponse.Ok(result);
    }

    @GetMapping("validateMeeting")
    public BaseResponse validateMeeting(@RequestParam(name = "access_token") String access_token) throws Exception {
        var result = _meetingService.validateMeetingService(access_token);
        return BaseResponse.Ok(result);
    }

    @PostMapping("validateMeetingIdPassCode")
    public BaseResponse validateMeetingIdPassCode(@RequestBody MeetingDetail meetingDetail) throws Exception {
        var result = _meetingService.validateMeetingIdPassCodeService(meetingDetail);
        return BaseResponse.Ok(result);
    }
}
