package cn.ac.iie.pkcgroup.dws.comm.request.user;

import cn.ac.iie.pkcgroup.dws.response.BasicResponse;
import cn.ac.iie.pkcgroup.dws.service.response.UserInfo;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
public class UserInfoResponse extends BasicResponse {
    UserInfo userInfo;
}
