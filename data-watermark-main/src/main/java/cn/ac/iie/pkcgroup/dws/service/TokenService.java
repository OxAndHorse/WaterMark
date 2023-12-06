package cn.ac.iie.pkcgroup.dws.service;

import cn.ac.iie.pkcgroup.dws.data.dao.AppRegisterInfoRepository;
import cn.ac.iie.pkcgroup.dws.data.dao.DemoUserRepository;
import cn.ac.iie.pkcgroup.dws.data.dao.entity.AppRegisterInfoEntity;
import cn.ac.iie.pkcgroup.dws.data.dao.entity.DemoUserEntity;
import cn.ac.iie.pkcgroup.dws.service.response.UserInfo;
import cn.ac.iie.pkcgroup.dws.utils.StringUtils;
import cn.ac.iie.pkcgroup.dws.utils.crypto.EncryptionUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static cn.ac.iie.pkcgroup.dws.Constants.*;

@Service
@Slf4j
public class TokenService {
    private AppRegisterInfoRepository appRegisterInfoRepository;
    private DemoUserRepository demoUserRepository;
    private String adminKeyCode;

    final int ADMIN_TOKEN = 0;
    final int USER_TOKEN = 1;
    final String TOKEN_CONN = "-";
    final String TOKEN_INNER_CONN = "_";

    @Autowired
    public void setAppRegisterInfoRepository(AppRegisterInfoRepository repository) {
        appRegisterInfoRepository = repository;
    }

    @Autowired
    public void setDemoUserRepository(DemoUserRepository repository) {
        demoUserRepository = repository;
    }

    private String generateRandomKeyCode() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[AUTH_KEY_LENGTH];
        random.nextBytes(bytes);
        return StringUtils.encodeToHex(bytes);
    }

    public int validateToken(String token) {
        int firstDivider = token.indexOf(TOKEN_CONN);
        int lastDivider = token.lastIndexOf(TOKEN_CONN);
        String username = token.substring(0, firstDivider);
        DemoUserEntity demoUser = demoUserRepository.findFirstById(username);
        if (demoUser == null) {
            log.info("No such user.");
            return -1;
        }
        if (demoUser.getRole() == ROLE_ADMIN) {
            if (lastDivider != firstDivider) return -1;
            String encToken = token.substring(lastDivider + 1);
            String message = EncryptionUtils.decrypt(encToken, adminKeyCode);
            if (message == null) return -1;
            firstDivider = message.indexOf(TOKEN_INNER_CONN);
            if (firstDivider < 0) return -1;
            String innerUsername = message.substring(0, firstDivider);
            if (!innerUsername.equals(username)) return -1;
            return ADMIN_TOKEN;
        }
        if (lastDivider == firstDivider) return -1;
        String recordSystemId = demoUser.getSystemId();
        String systemId = token.substring(firstDivider + 1, lastDivider);
        if (!recordSystemId.equals(systemId)) return -1;
        AppRegisterInfoEntity appRegisterInfo = appRegisterInfoRepository.findFirstBySystemId(recordSystemId);
        if (appRegisterInfo == null) {
            log.info("No such system.");
            return -1;
        }
        String authKey = appRegisterInfo.getSystemAuthKey();
        String encToken = token.substring(lastDivider + 1);
        String message = EncryptionUtils.decrypt(encToken, authKey);
        if (message == null) return -1;
        firstDivider = message.indexOf(TOKEN_INNER_CONN);
        lastDivider = message.lastIndexOf(TOKEN_INNER_CONN);
        if (firstDivider < 0 || firstDivider == lastDivider) return -1;
        if (!username.equals(message.substring(0, firstDivider))) return -1;
        if (!systemId.equals(message.substring(firstDivider + 1, lastDivider))) return -1;
        Pattern pattern = Pattern.compile("^[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}$");
        Matcher matcher = pattern.matcher(systemId);
        if (!matcher.find()) return -1; // Illegal systemId

        return USER_TOKEN;
    }

    public String generateTokenByUsername(String username) {
        DemoUserEntity demoUser = demoUserRepository.findFirstById(username);
        if (demoUser == null) {
            log.info("No such user.");
            return null;
        }
        String message, systemId = null, authKey;
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timestamp = df.format(new Date());
        if (demoUser.getRole() == ROLE_ADMIN) {
            if (adminKeyCode == null) {
                adminKeyCode = generateRandomKeyCode();
            }
            authKey = adminKeyCode;
            message = username + TOKEN_INNER_CONN + timestamp;
        } else {
            systemId = demoUser.getSystemId();
            AppRegisterInfoEntity appRegisterInfo = appRegisterInfoRepository.findFirstBySystemId(systemId);
            if (appRegisterInfo == null) {
                log.info("No such system.");
                return null;
            }
            authKey = appRegisterInfo.getSystemAuthKey();
            message = username + TOKEN_INNER_CONN + systemId + TOKEN_INNER_CONN + timestamp;
        }
        String encToken = EncryptionUtils.encrypt(message, authKey);
        return systemId == null ? username + TOKEN_CONN + encToken : username + TOKEN_CONN + systemId + TOKEN_CONN + encToken;
    }

    public String parseSystemId(String token) {
        int code = validateToken(token);
        if (code < 0 || code == ADMIN_TOKEN) return null; // admin did not hold system id
        int lastDivider = token.lastIndexOf(TOKEN_CONN);
        int firstDivider = token.indexOf(TOKEN_CONN);
        String systemId = token.substring(firstDivider + 1, lastDivider);
        String encToken = token.substring(lastDivider + 1);
        AppRegisterInfoEntity appRegisterInfo = appRegisterInfoRepository.findFirstBySystemId(systemId);
        String message = EncryptionUtils.decrypt(encToken, appRegisterInfo.getSystemAuthKey());
        assert message != null;
        firstDivider = message.indexOf(TOKEN_INNER_CONN);
        lastDivider = message.lastIndexOf(TOKEN_INNER_CONN);
        return message.substring(firstDivider + 1, lastDivider);
    }

    public String parseSystemName(String token) {
        int firstDivider = token.indexOf(TOKEN_CONN);
        String username = token.substring(0, firstDivider);
        DemoUserEntity demoUser = demoUserRepository.findFirstById(username);
        if (demoUser.getRole() == ROLE_OWNER) {
            AppRegisterInfoEntity appRegisterInfo = appRegisterInfoRepository.findFirstBySystemId(demoUser.getSystemId());
            return appRegisterInfo.getSystemNickname(); // return Chinese name
        } else {
            return demoUser.getNickname();
        }
    }

    public boolean isAdmin(String token) {
        int firstDivider = token.indexOf(TOKEN_CONN);
        String username = token.substring(0, firstDivider);
        DemoUserEntity demoUser = demoUserRepository.findFirstById(username);
        if (demoUser == null) {
            log.info("No such user.");
            return false;
        }
        return demoUser.getRole() == ROLE_ADMIN;
    }

    public boolean isOwner(String token) {
        int firstDivider = token.indexOf(TOKEN_CONN);
        String username = token.substring(0, firstDivider);
        DemoUserEntity demoUser = demoUserRepository.findFirstById(username);
        if (demoUser == null) {
            log.info("No such user.");
            return false;
        }
        return demoUser.getRole() == ROLE_OWNER;
    }


    public String parseUserNickname(String token) {
        int firstDivider = token.indexOf(TOKEN_CONN);
        String username = token.substring(0, firstDivider);
        DemoUserEntity demoUser = demoUserRepository.findFirstById(username);
        if (demoUser == null) {
            log.info("No such user.");
            return null;
        }
        return demoUser.getRole() == ROLE_USER ? demoUser.getNickname() : "";
    }

    public UserInfo parseUserInfo(String token) {
        int code = validateToken(token);
        if (code < 0) return null; // invalid token
        int firstDivider = token.indexOf(TOKEN_CONN);
        String username = token.substring(0, firstDivider);
        DemoUserEntity demoUser = demoUserRepository.findFirstById(username);
        if (demoUser == null) {
            log.info("No such user.");
            return null;
        }
        ArrayList<String> roles = getRoles(demoUser.getRole());
        UserInfo userInfo = new UserInfo();
        userInfo.setRoles(roles);
        userInfo.setName(demoUser.getNickname());
        return userInfo;
    }

    private ArrayList<String> getRoles(int roleCode) {
        ArrayList<String> roles = new ArrayList<>(1);
        String role = ROLE_MAP.get(roleCode);
        if (role == null) return null;
        roles.add(role);
        return roles;
    }
}
