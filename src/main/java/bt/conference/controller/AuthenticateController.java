package bt.conference.controller;

import bt.conference.entity.LoginDetail;
import bt.conference.serviceinterface.IAuthenticateService;
import com.fierhub.model.ApiAuthResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/auth/")
public class AuthenticateController {
    @Autowired
    IAuthenticateService _authService;

    @PostMapping("authenticateUser")
    public ApiAuthResponse authenticateUser(@RequestBody LoginDetail loginDetail) throws Exception {
        return _authService.authenticateUserService(loginDetail);
    }

    @PostMapping("regenerateToken")
    public ApiAuthResponse regenerateToken(HttpServletRequest request) throws Exception {
        return _authService.regenerateTokenService(request);
    }
}
