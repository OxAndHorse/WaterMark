package cn.ac.iie.pkcgroup.dws.comm.response;

import cn.ac.iie.pkcgroup.dws.response.BasicResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class UserRegisterResponse extends BasicResponse {
    String userPassword;
}
