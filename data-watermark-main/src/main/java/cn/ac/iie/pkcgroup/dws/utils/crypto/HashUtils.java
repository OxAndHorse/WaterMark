package cn.ac.iie.pkcgroup.dws.utils.crypto;

import cn.ac.iie.pkcgroup.dws.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Slf4j
public class HashUtils {
    private static final String SHA256 = "SHA-256";

    public static byte[] doHash(String msg) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(SHA256);
            messageDigest.update(msg.getBytes(StandardCharsets.UTF_8));
            return messageDigest.digest();
        } catch (NoSuchAlgorithmException e) {
            log.error("No algorithm " + SHA256);
            return null;
        }
    }

    public static byte[] doHash(byte[] msg) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(SHA256);
            messageDigest.update(msg);
            return messageDigest.digest();
        } catch (NoSuchAlgorithmException e) {
            log.error("No algorithm " + SHA256);
            return null;
        }
    }

    public static String doHashToHex(byte[] msg) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(SHA256);
            messageDigest.update(msg);
            byte[] hash = messageDigest.digest();
            return StringUtils.encodeToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("No algorithm " + SHA256);
            return null;
        }
    }
}
