package cn.ac.iie.pkcgroup.dws.route;

import cn.ac.iie.pkcgroup.dws.comm.request.user.UserInfoResponse;
import cn.ac.iie.pkcgroup.dws.comm.request.user.UserRegisterRequest;
import cn.ac.iie.pkcgroup.dws.comm.request.user.UserLoginRequest;
import cn.ac.iie.pkcgroup.dws.comm.response.UserLoginResponse;
import cn.ac.iie.pkcgroup.dws.comm.response.UserRegisterResponse;
import cn.ac.iie.pkcgroup.dws.comm.token.DWSCookie;
import cn.ac.iie.pkcgroup.dws.handler.user.UserHandler;
import cn.ac.iie.pkcgroup.dws.handler.user.response.UserResponse;
import cn.ac.iie.pkcgroup.dws.response.BasicResponse;
import cn.ac.iie.pkcgroup.dws.comm.response.StatusCodes;
//import jakarta.servlet.http.HttpServletResponse;
import cn.ac.iie.pkcgroup.dws.service.TokenService;
import cn.ac.iie.pkcgroup.dws.service.response.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import static cn.ac.iie.pkcgroup.dws.Constants.NORMAL_CODE;

@CrossOrigin(value = "*")
@RestController
@RequestMapping("/dws/user")
public class UserController {
    final static String COOKIE_NAME = "dwsToken";

    private TokenService tokenService;
    private UserHandler userHandler;

    @Autowired
    public void setTokenService(TokenService service) {
        tokenService = service;
    }

    @Autowired
    public void setUserHandler(UserHandler handler) {
        userHandler = handler;
    }

    @PostMapping(value = "/loginBackend")
    public BasicResponse login(
            @RequestBody @Valid UserLoginRequest userLoginRequest,
            HttpServletResponse servletResponse
    ) {
        String username = userLoginRequest.getUsername();
        String password = userLoginRequest.getPassword();
        if (!userHandler.authUser(username, password)) return new BasicResponse(StatusCodes.CODE_AUTH_FAIL, StatusCodes.MSG_AUTH_FAIL);

        DWSCookie dwsCookie = new DWSCookie();
        String token = tokenService.generateTokenByUsername(username);
        if (token == null) {
            return new BasicResponse(StatusCodes.CODE_AUTH_FAIL, StatusCodes.MSG_AUTH_FAIL);
        }

        servletResponse.addCookie(dwsCookie.generateDWSCookie(COOKIE_NAME, token));
        return UserLoginResponse.builder()
                .statusCode(StatusCodes.CODE_SUCCESS)
                .message(StatusCodes.MSG_SUCCESS)
                .dwsToken(token)
                .build();
    }

    @GetMapping(value = "/info")
    public BasicResponse getUserInfo(
            @RequestHeader("X-USER-TOKEN") String token
    ) {
        UserInfo userInfo = tokenService.parseUserInfo(token);
        if (userInfo == null) new BasicResponse(StatusCodes.CODE_AUTH_FAIL, StatusCodes.MSG_AUTH_FAIL);

        return UserInfoResponse.builder()
                .statusCode(StatusCodes.CODE_SUCCESS)
                .message(StatusCodes.MSG_SUCCESS)
                .userInfo(userInfo)
                .build();
    }

    @GetMapping(value = "/logoutBackend")
    public BasicResponse logout(
            @RequestHeader("X-USER-TOKEN") String token
    ) {
        return new BasicResponse(StatusCodes.CODE_SUCCESS, StatusCodes.MSG_SUCCESS);
    }

    @PostMapping(value = "/register")
    public BasicResponse register(
            @RequestHeader("X-USER-TOKEN") String token,
            @RequestBody @Valid UserRegisterRequest userRegisterRequest
            ) {
        if (!tokenService.isAdmin(token)) return new BasicResponse(StatusCodes.CODE_UNAUTHORIZED_REQUEST, StatusCodes.MSG_UNAUTHORIZED_REQUEST);
        UserResponse userResponse = userHandler.generateUser(userRegisterRequest);
        if (userResponse.statusCode == NORMAL_CODE) {
            return UserRegisterResponse.builder()
                    .statusCode(userResponse.getStatusCode())
                    .message(userResponse.getMessage())
                    .userPassword(userResponse.getPassword())
                    .build();
        }
        return userResponse;
    }
}
