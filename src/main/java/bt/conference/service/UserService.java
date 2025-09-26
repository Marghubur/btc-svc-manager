package bt.conference.service;

import bt.conference.entity.UserDetail;
import bt.conference.model.FilterModel;
import bt.conference.serviceinterface.IUserService;
import in.bottomhalf.ps.database.utils.DbParameters;
import in.bottomhalf.ps.database.utils.DbProcedureManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Types;
import java.util.Arrays;
import java.util.List;

@Service
public class UserService implements IUserService {
    @Autowired
    DbProcedureManager dbProcedureManager;
    public List<UserDetail> getAllUserService(FilterModel filterModel) throws Exception {
        return dbProcedureManager.getRecords("sp_get_user_by_filter",
                Arrays.asList(
                        new DbParameters("_searchString", filterModel.getSearchString(), Types.VARCHAR),
                        new DbParameters("_sortBy", filterModel.getSortBy(), Types.VARCHAR),
                        new DbParameters("_pageIndex", filterModel.getPageIndex(), Types.INTEGER),
                        new DbParameters("_pageSize", filterModel.getPageSize(), Types.INTEGER)
                ), UserDetail.class
        );
    }
}
