package cn.ac.iie.pkcgroup.dws.comm.request.user;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class UserLoginRequest {
    @NotNull(message = "username should not be null.")
    String username;
    @NotNull(message = "password should not be null.")
    String password;
}
