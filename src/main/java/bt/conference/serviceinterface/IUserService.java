package bt.conference.serviceinterface;

import bt.conference.entity.UserDetail;
import bt.conference.model.FilterModel;

import java.util.List;

public interface IUserService {
    List<UserDetail> getAllUserService(FilterModel filterModel) throws Exception;
}
