package cn.ac.iie.pkcgroup.dws.utils.crypto;

import cn.ac.iie.pkcgroup.dws.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.spec.KeySpec;
import java.text.SimpleDateFormat;
import java.util.Date;

@Slf4j
public class EncryptionUtils {
    static final int DEFAULT_ENC_KEY_LENGTH = 128;
    static final String DEFAULT_ENC_ALGORITHM = "AES";
    static final String DEFAULT_ENC_ALGORITHM_MODE = "AES/GCM/PKCS5Padding";
    static final String DEFAULT_KDF_ALGORITHM = "PBKDF2WithHmacSHA256";
    static final int DEFAULT_ROUND = 1000;
    static final int DEFAULT_IV_LENGTH = 12;

    public static String encrypt(String message, String keyCode) {

        int length = keyCode.length();
        String pwd = keyCode.substring(0, length / 2);
        String salt = keyCode.substring(length / 2 + 1);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(DEFAULT_KDF_ALGORITHM);
            KeySpec spec = new PBEKeySpec(pwd.toCharArray(), salt.getBytes(StandardCharsets.UTF_8), DEFAULT_ROUND, DEFAULT_ENC_KEY_LENGTH);
            SecretKey sk = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), DEFAULT_ENC_ALGORITHM);
            Cipher cipher = Cipher.getInstance(DEFAULT_ENC_ALGORITHM_MODE);
            cipher.init(Cipher.ENCRYPT_MODE, sk);
            byte[] iv = cipher.getIV();
            byte[] cipherText = cipher.doFinal(message.getBytes());
            String target = StringUtils.encodeToHex(iv);
            target += StringUtils.encodeToHex(cipherText);
            return target;
        } catch (Exception e) {
            log.error("Encryption error.", e);
            return null;
        }
    }

    public static String decrypt(String ciphertext, String keyCode) {
        int length = keyCode.length();
        String pwd = keyCode.substring(0, length / 2);
        String salt = keyCode.substring(length / 2 + 1);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(DEFAULT_KDF_ALGORITHM);
            KeySpec spec = new PBEKeySpec(pwd.toCharArray(), salt.getBytes(StandardCharsets.UTF_8), DEFAULT_ROUND, DEFAULT_ENC_KEY_LENGTH);
            SecretKey sk = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), DEFAULT_ENC_ALGORITHM);
            Cipher cipher = Cipher.getInstance(DEFAULT_ENC_ALGORITHM_MODE);
            byte[] source = StringUtils.decodeFromHex(ciphertext);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(DEFAULT_ENC_KEY_LENGTH, source, 0, DEFAULT_IV_LENGTH);
            cipher.init(Cipher.DECRYPT_MODE, sk, gcmParameterSpec);
            byte[] message = cipher.doFinal(source, DEFAULT_IV_LENGTH, source.length - DEFAULT_IV_LENGTH);
            return new String(message, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Encryption error.", e);
            return null;
        }
    }
}
