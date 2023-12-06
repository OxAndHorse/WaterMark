package cn.ac.iie.pkcgroup.dws.utils.crypto;

import org.springframework.util.Base64Utils;

import java.security.SecureRandom;

public class PasswordUtils {
    static final int DEFAULT_LENGTH = 6;
    static final String DEFAULT_PWD = "123456";

    public static String generateRandomPassword(int length, boolean useDefault) {
        int realLength = Math.max(length, DEFAULT_LENGTH);
        if (useDefault) return DEFAULT_PWD;
        SecureRandom secureRandom = new SecureRandom();
        byte[] seed = new byte[realLength];
        secureRandom.nextBytes(seed);
        return Base64Utils.encodeToString(seed);
    }
}
