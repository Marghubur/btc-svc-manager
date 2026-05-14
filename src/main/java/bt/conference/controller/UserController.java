package bt.conference.controller;

import bt.conference.serviceinterface.IUserService;
import com.fierhub.model.BaseResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/user/")
public class UserController {
    @Autowired
    IUserService _userService;

    @GetMapping("getAllUser")
    public BaseResponse getAllUser() throws Exception {
        var result = _userService.getAllUserService();
        return BaseResponse.Ok(result);
    }
}
