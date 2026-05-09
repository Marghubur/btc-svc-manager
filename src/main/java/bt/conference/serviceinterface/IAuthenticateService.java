package bt.conference.serviceinterface;

import bt.conference.entity.LoginDetail;
import com.fierhub.model.ApiAuthResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface IAuthenticateService {
    ApiAuthResponse authenticateUserService(LoginDetail loginDetail) throws Exception;
    ApiAuthResponse authenticateMobileUserService(LoginDetail loginDetail) throws Exception;
    ApiAuthResponse regenerateTokenService(HttpServletRequest request) throws Exception;
    ApiAuthResponse generateAccessTokenService(HttpServletRequest request) throws Exception;
}
