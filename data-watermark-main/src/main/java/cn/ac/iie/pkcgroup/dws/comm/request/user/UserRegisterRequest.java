package cn.ac.iie.pkcgroup.dws.comm.request.user;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class UserRegisterRequest {
    @NotNull(message = "username should not be null.")
    String username;
    @NotNull(message = "userNickname should not be null.")
    String userNickname;
    @NotNull(message = "systemId should not be null.")
    String systemId;
    int functions; // preserved.
}
