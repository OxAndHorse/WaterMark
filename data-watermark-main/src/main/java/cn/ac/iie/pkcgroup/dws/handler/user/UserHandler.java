package cn.ac.iie.pkcgroup.dws.handler.user;

import cn.ac.iie.pkcgroup.dws.comm.request.user.UserRegisterRequest;
import cn.ac.iie.pkcgroup.dws.comm.response.StatusCodes;
import cn.ac.iie.pkcgroup.dws.data.dao.AppRegisterInfoRepository;
import cn.ac.iie.pkcgroup.dws.data.dao.DemoUserRepository;
import cn.ac.iie.pkcgroup.dws.data.dao.entity.AppRegisterInfoEntity;
import cn.ac.iie.pkcgroup.dws.data.dao.entity.DemoUserEntity;
import cn.ac.iie.pkcgroup.dws.handler.app.response.AppStatusCodes;
import cn.ac.iie.pkcgroup.dws.handler.user.response.UserResponse;
import cn.ac.iie.pkcgroup.dws.utils.crypto.HashUtils;
import cn.ac.iie.pkcgroup.dws.utils.crypto.PasswordUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

import static cn.ac.iie.pkcgroup.dws.Constants.ROLE_USER;

@Component
public class UserHandler {
    private DemoUserRepository demoUserRepository;
    private AppRegisterInfoRepository appRegisterInfoRepository;

    @Autowired
    public void setDemoUserRepository(DemoUserRepository repository) {
        demoUserRepository = repository;
    }

    @Autowired
    public void setAppRegisterInfoRepository(AppRegisterInfoRepository repository) {
        appRegisterInfoRepository = repository;
    }

    public boolean authUser(String username, String password) {
        DemoUserEntity demoUser = demoUserRepository.findFirstById(username);
        return demoUser != null && demoUser.getPassword().equals(password);
    }

    public UserResponse generateUser(UserRegisterRequest userRegisterRequest) {
        AppRegisterInfoEntity appRegisterInfo = appRegisterInfoRepository.findFirstBySystemId(userRegisterRequest.getSystemId());
        if (appRegisterInfo == null) return UserResponse.builder()
                .statusCode(AppStatusCodes.CODE_UNREGISTERED_SYSTEM)
                .message(AppStatusCodes.MSG_UNREGISTERED_SYSTEM)
                .build();
        DemoUserEntity demoUser = new DemoUserEntity();
        demoUser.setId(userRegisterRequest.getUsername());
        demoUser.setSystemId(userRegisterRequest.getSystemId());
        demoUser.setRole(ROLE_USER);
        demoUser.setNickname(userRegisterRequest.getUserNickname());
        String password = PasswordUtils.generateRandomPassword(0, true);
        demoUser.setPassword(HashUtils.doHashToHex(password.getBytes(StandardCharsets.UTF_8)));
        demoUser.setUid(HashUtils.doHashToHex(userRegisterRequest.getUsername().getBytes(StandardCharsets.UTF_8)));
        demoUserRepository.saveAndFlush(demoUser);
        return UserResponse.builder()
                .statusCode(StatusCodes.CODE_SUCCESS)
                .message(StatusCodes.MSG_SUCCESS)
                .password(password)
                .build();
    }
}
