package cn.ac.iie.pkcgroup.dws.comm.response.app;

import cn.ac.iie.pkcgroup.dws.response.BasicResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class RegisterResponse extends BasicResponse {
    String systemPassword;
    String apiUser;
    String apiAuthKey;
}
