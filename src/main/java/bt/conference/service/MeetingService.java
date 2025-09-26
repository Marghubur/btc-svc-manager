package bt.conference.service;

import bt.conference.entity.MeetingDetail;
import bt.conference.entity.UserDetail;
import bt.conference.serviceinterface.IMeetingService;
import com.fierhub.model.CurrentSession;
import in.bottomhalf.ps.database.service.DbManager;
import in.bottomhalf.ps.database.utils.DbParameters;
import in.bottomhalf.ps.database.utils.DbProcedureManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Types;
import java.util.Arrays;
import java.util.List;

@Service
public class MeetingService implements IMeetingService {
    @Autowired
    CurrentSession currentSession;
    @Autowired
    DbProcedureManager dbProcedureManager;
    @Autowired
    DbManager dbManager;
    public List<MeetingDetail> generateMeetingService(MeetingDetail meetingDetail) throws Exception {
        if (meetingDetail.getTitle() == null || meetingDetail.getTitle().isEmpty())
            throw new Exception("Invalid meeting title");

        if (meetingDetail.getDurationInSecond() <= 0)
            throw new Exception("Invalid meeting duration");

        var convertedDate = UtilService.toUtc(meetingDetail.getStartDate());
        meetingDetail.setStartDate(convertedDate);
        meetingDetail.setHasQuickMeeting(false);
        addMeetingDetail(meetingDetail);

        return getAllMeetingByOrganizerService();
    }

    private void addMeetingDetail(MeetingDetail meetingDetail) throws Exception {
        var user = dbManager.getById(currentSession.getUserId(), UserDetail.class);
        if (user == null)
            throw new Exception("User not found");

        var fullName = user.getFirstName() + (user.getLastName() != null && !user.getLastName().isEmpty() ? " " + user.getLastName() : "");
        var nextId = dbManager.nextLongPrimaryKey(MeetingDetail.class);
        meetingDetail.setMeetingDetailId(nextId);
        meetingDetail.setMeetingId(UtilService.encodeUsingSecretOnly(nextId));
        meetingDetail.setMeetingPassword(UtilService.generatePassword(6));
        meetingDetail.setOrganizedBy(currentSession.getUserId());

        dbManager.save(meetingDetail);
    }

    public List<MeetingDetail> getAllMeetingByOrganizerService() throws Exception {

        return dbProcedureManager.getRecords("sp_get_all_meeting_userid",
                Arrays.asList(
                        new DbParameters("_userid", currentSession.getUserId(), Types.BIGINT)
                ),
                MeetingDetail.class
        );
    }

    public List<MeetingDetail> generateQuickMeetingService(MeetingDetail meetingDetail) throws Exception {
        meetingDetail.setDurationInSecond(36000);
        java.util.Date utilDate = new java.util.Date();
        var date = new java.sql.Timestamp(utilDate.getTime());
        meetingDetail.setStartDate(date);
        meetingDetail.setHasQuickMeeting(true);

        addMeetingDetail(meetingDetail);
        return getAllMeetingByOrganizerService();
    }

    public  MeetingDetail validateMeetingService(MeetingDetail meetingDetail) throws Exception {
        if (meetingDetail.getMeetingId() == null || meetingDetail.getMeetingId().isEmpty())
            throw new Exception("Invalid meeting id passed");

        var meetingDetailId = UtilService.decodeUsingSecretOnly(meetingDetail.getMeetingId());
        var existingMeetingDetail = dbManager.getById(meetingDetailId, MeetingDetail.class);
        if (existingMeetingDetail == null)
            throw new Exception("Meeting detail not found");

        if (!meetingDetail.getMeetingId().equals(existingMeetingDetail.getMeetingId()))
            throw new Exception("Invalid meeting id");

        return existingMeetingDetail;
    }

    public MeetingDetail validateMeetingIdPassCodeService(MeetingDetail meetingDetail) throws Exception {
        if (meetingDetail.getMeetingPassword() == null || meetingDetail.getMeetingPassword().isEmpty())
            throw new Exception("Invalid meeting passcode");

        if (meetingDetail.getMeetingId() == null || meetingDetail.getMeetingId().isEmpty())
            throw new Exception("Invalid meeting id passed");
        var meetingDetailId = UtilService.decodeUsingSecretOnly(meetingDetail.getMeetingId());
        var existingMeetingDetail = dbManager.getById(meetingDetailId , MeetingDetail.class);
        if (existingMeetingDetail == null)
            throw new Exception("Meeting detail not found");

        if (!existingMeetingDetail.getMeetingPassword().equals(meetingDetail.getMeetingPassword()))
            throw new Exception("Invalid meeting passcode. Please contact to admin");

        return existingMeetingDetail;
    }
}