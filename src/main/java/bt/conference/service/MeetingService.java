package bt.conference.service;

import bt.conference.dto.PagedResponse;
import bt.conference.entity.Conversation;
import bt.conference.entity.MeetingDetail;
import bt.conference.entity.UserDetail;
import bt.conference.model.GuestMeeting;
import bt.conference.model.TokenStatus;
import bt.conference.repository.ConversationRepository;
import bt.conference.serviceinterface.IMeetingService;
import com.fierhub.database.service.DbManager;
import com.fierhub.database.utils.DbParameters;
import com.fierhub.database.utils.ProcedureManager;
import com.fierhub.model.UserSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.Types;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MeetingService implements IMeetingService {
    @Autowired
    UserSession userSession;
    @Autowired
    ProcedureManager dbProcedureManager;
    @Autowired
    DbManager dbManager;
    @Autowired
    ConversationService conversationService;
    @Autowired
    DbResultMapper resultMapper;
    @Autowired
    ConversationRepository conversationRepository;

    private static final String CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789abcdefghijkmnopqrstuvwxyz";
    private static final SecureRandom random = new SecureRandom();

    public Map<String, Object> generateMeetingService(MeetingDetail meetingDetail) throws Exception {
        if (meetingDetail.getTitle() == null || meetingDetail.getTitle().isEmpty())
            throw new Exception("Invalid meeting title");

        if (meetingDetail.getDurationInSecond() <= 0)
            throw new Exception("Invalid meeting duration");

        var convertedDate = UtilService.toUtc(meetingDetail.getStartDate());
        meetingDetail.setStartDate(convertedDate);
        meetingDetail.setHasQuickMeeting(false);

        // Create corresponding Conversation in MongoDB so Go WebSocket / Rooms can load it
        var conversation = conversationService.createMeetingConversationService(userSession.getUserId(),
                meetingDetail.getTitle(), meetingDetail.getParticipantsId());
        meetingDetail.getParticipantsId().add(userSession.getUserId());
        meetingDetail.setParticipants(resultMapper.writeValueAsString(meetingDetail.getParticipantsId()));
        meetingDetail.setConversationId(conversation.getId());

        addMeetingDetail(meetingDetail);

        return getAllMeetingByOrganizerService();
    }

    private void addMeetingDetail(MeetingDetail meetingDetail) throws Exception {
        var userId = UtilService.extractEmployeeId(userSession.getUserId(), userSession.getClaimsValue().get("code"));
        var user = dbManager.getById(userId, UserDetail.class);
        if (user == null)
            throw new Exception("User not found");

        var fullName = user.getFirstName() + (user.getLastName() != null && !user.getLastName().isEmpty() ? " " + user.getLastName() : "");
        meetingDetail.setMeetingId(ManageMeetingService.generateToken(userId, fullName));
        meetingDetail.setMeetingPassword(generatePassword(6));
        meetingDetail.setOrganizedBy(userId);

        dbManager.save(meetingDetail);
    }

    public PagedResponse<Conversation> getRecentMeetingsService() throws Exception {
        var code = userSession.getClaimsValue().get("code");
        if (code == null || code.isEmpty())
            throw new Exception("Invalid user session");

        int userId = Integer.parseInt(userSession.getUserId().replace(code, ""));
        return conversationService.searchConversationsRecentGroup(String.valueOf(userId), 1, 5);
    }

    public Map<String, Object> generateQuickMeetingService(MeetingDetail meetingDetail) throws Exception {
        meetingDetail.setDurationInSecond(36000);
        java.util.Date utilDate = new java.util.Date();
        var date = new java.sql.Timestamp(utilDate.getTime());
        meetingDetail.setStartDate(date);
        meetingDetail.setHasQuickMeeting(true);

        var conversation = conversationService.createMeetingConversationService(userSession.getUserId(), meetingDetail.getTitle(), List.of());
        meetingDetail.getParticipantsId().add(userSession.getUserId());
        meetingDetail.setParticipants(resultMapper.writeValueAsString(meetingDetail.getParticipantsId()));
        meetingDetail.setConversationId(conversation.getId());

        addMeetingDetail(meetingDetail);
        return getAllMeetingByOrganizerService();
    }

    public Conversation validateMeetingService(String access_token) throws Exception {
        if (access_token == null || access_token.isEmpty())
            throw new Exception("Invalid access token used");

        try {
            access_token = java.net.URLDecoder.decode(access_token, StandardCharsets.UTF_8);
            Date nowUtc = new Date();
            GuestMeeting guestMeeting = dbProcedureManager.execute("sp_get_guest_access",
                    List.of(
                            new DbParameters("p_access_token", access_token, Types.VARCHAR)
                    ), GuestMeeting.class
            );

            GuestMeeting validToken = validateGuestToken(guestMeeting, nowUtc);

            var conv = conversationRepository.findById(guestMeeting.getMeetingId());

            if (conv.isEmpty())
                throw new Exception("Meeting detail not found");

            return conv.get();
        } catch (Exception ex) {
            throw new Exception("Fail to get record from access token");
        }
    }

    private GuestMeeting validateGuestToken(
            GuestMeeting token,
            Date requestTimeUtc
    ) throws Exception {

        // 1️⃣ Token existence check
        if (token == null) {
            throw new Exception("Invalid or expired access token");
        }

        // 2️⃣ Token status check
        if (token.getStatus() != TokenStatus.ACTIVE) {
            throw new Exception("Access token is not active");
        }

        // 3️⃣ Time window validation (UTC)
        if (requestTimeUtc.before(token.getValidFrom())) {
            throw new Exception("Access token is not valid yet");
        }

        if (requestTimeUtc.after(token.getValidUntil())) {
            throw new Exception("Access token has expired");
        }

        // 4️⃣ Usage count validation
        if (token.getMaxUsage() != null) {
            int used = token.getUsageCount() == null ? 0 : token.getUsageCount();

            if (used >= token.getMaxUsage()) {
                throw new Exception("Access token usage limit exceeded");
            }
        }

        // 5️⃣ Defensive checks (optional but recommended)
        if (token.getMeetingId() == null) {
            throw new Exception("Invalid token mapping");
        }

        // 6️⃣ Token is valid → allow access
        return token;
    }

    private void markTokenUsed(GuestMeeting token) {
        token.setUsageCount(
                token.getUsageCount() == null ? 1 : token.getUsageCount() + 1
        );

        if (token.getMaxUsage() != null &&
                token.getUsageCount() >= token.getMaxUsage()) {
            token.setStatus(TokenStatus.EXPIRED);
        }

        // guestTokenRepository.save(token);
    }

    private String generatePassword(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }

    public MeetingDetail validateMeetingIdPassCodeService(MeetingDetail meetingDetail) throws Exception {
        if (meetingDetail.getMeetingPassword() == null || meetingDetail.getMeetingPassword().isEmpty())
            throw new Exception("Invalid meeting passcode");

        if (meetingDetail.getMeetingId() == null || meetingDetail.getMeetingId().isEmpty())
            throw new Exception("Invalid meeting id passed");

        var existingMeeting = dbManager.queryRaw("select * from meeting_detail where meetingId = '"
                + meetingDetail.getMeetingId() + "' and meetingPassword = '"
                + meetingDetail.getMeetingPassword() + "'" , MeetingDetail.class);
        if (existingMeeting == null)
            throw new Exception("Meeting detail not found");

        if (existingMeeting.getConversationId() == null || existingMeeting.getConversationId().isEmpty())
            throw new Exception("Conversation detail not found");

        var user = dbManager.getById(String.valueOf(existingMeeting.getOrganizedBy()), UserDetail.class);
        if (user == null)
            throw new Exception("Admin detail not found");

        existingMeeting.setOrganizerName(user.getFirstName() + (user.getLastName() != null && !user.getLastName().isEmpty() ? " " + user.getLastName() : ""));
        return existingMeeting;
    }

    public Map<String, Object> getAllMeetingByOrganizerService() throws Exception {
        long currentUserId;
        try {
            currentUserId = Long.parseLong(userSession.getUserId());
        } catch (Exception e) {
            var code = userSession.getClaimsValue().get("code");
            currentUserId = UtilService.extractEmployeeId(userSession.getUserId(), code);
        }

        var result = dbProcedureManager.executeProcedure("sp_dashboard_get_by_userid",
                List.of(new DbParameters("_userid", currentUserId, Types.BIGINT))
        );
        Map<String, Object> data = new HashMap<>();
        data.put("QuickMeetings", resultMapper.mapListResult(result, "#result-set-1", MeetingDetail.class));
        data.put("ScheduledMeetings", resultMapper.mapListResult(result, "#result-set-2", MeetingDetail.class));
        return data;
    }

    public List<MeetingDetail> getAllScheduleMeetingByOrganizerService() throws Exception {
        var result = dbProcedureManager.executeProcedure("sp_get_meetings_by_participant",
                List.of(new DbParameters("_userid", userSession.getUserId(), Types.VARCHAR))
        );
        return resultMapper.mapListResult(result, "#result-set-1", MeetingDetail.class);
    }

    public MeetingDetail validateMeetingByIdService(String meetingId) throws Exception {
        if (meetingId == null || meetingId.isEmpty() || "undefined".equals(meetingId) || "undefined_undefined".equals(meetingId))
            throw new Exception("Invalid meeting id passed");

        var targetMeetingId = "";
        long targetDetailId = 0L;
        int lastUnderscore = meetingId.lastIndexOf('_');

        if (lastUnderscore != -1) {
            targetMeetingId = meetingId.substring(0, lastUnderscore);
            targetDetailId = Long.parseLong(meetingId.substring(lastUnderscore + 1));
        }
        var meetingDetail = dbManager.queryRaw("select * from meeting_detail where meetingDetailId = " + targetDetailId , MeetingDetail.class);
        if (meetingDetail == null || !meetingDetail.getMeetingId().equals(targetMeetingId))
            throw new Exception("invalid meeting link");

        if (meetingDetail.getConversationId() == null || meetingDetail.getConversationId().isEmpty())
            throw new Exception("Conversation detail not found");

        var user = dbManager.getById(String.valueOf(meetingDetail.getOrganizedBy()), UserDetail.class);
        if (user == null)
            throw new Exception("Admin detail not found");

        meetingDetail.setOrganizerName(user.getFirstName() + (user.getLastName() != null && !user.getLastName().isEmpty() ? " " + user.getLastName() : ""));
        return meetingDetail;
    }
}
