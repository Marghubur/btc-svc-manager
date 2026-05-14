package bt.conference.service;

import bt.conference.entity.UserDetail;
import bt.conference.serviceinterface.IUserService;
import com.fierhub.database.service.DbManager;
import com.fierhub.database.utils.ProcedureManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService implements IUserService {
    @Autowired
    DbManager dbManager;
    @Autowired
    ProcedureManager procedureManager;


    public List<UserDetail> getAllUserService() throws Exception {
        return dbManager.get(UserDetail.class);
    }
}
