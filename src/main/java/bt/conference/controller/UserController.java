package bt.conference.controller;

import bt.conference.model.FilterModel;
import bt.conference.serviceinterface.IUserService;
import in.bottomhalf.common.models.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/user/")
public class UserController {
    @Autowired
    IUserService _userService;
    @PostMapping("getAllUser")
    public ResponseEntity<ApiResponse> getAllUser(@RequestBody FilterModel filterModel) throws Exception {
        var result = _userService.getAllUserService(filterModel);
        return ResponseEntity.ok(ApiResponse.Ok(result));
    }
}
