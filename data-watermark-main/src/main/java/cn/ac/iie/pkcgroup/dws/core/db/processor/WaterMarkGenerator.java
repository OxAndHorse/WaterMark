package cn.ac.iie.pkcgroup.dws.core.db.processor;

import cn.ac.iie.pkcgroup.dws.core.db.model.Watermark;
import cn.ac.iie.pkcgroup.dws.core.WatermarkException;
import cn.ac.iie.pkcgroup.dws.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Random;

/**
 * Get WaterMark
 */
@Slf4j
public class WaterMarkGenerator {
    private static final int MAX_TIMES = 1 << 16;
    private static final Random r = new Random();
    // 水印位数
    // TODO: read from config file
    public static final int WATERMARK_LENGTH = 128; // 128bit?
    // 越低越严格
    private static final double SAME_THRESHOLD = 0.7;

    private static ArrayList<Integer> toBinary(String string) {
        char[] tmp = string.toCharArray();
        ArrayList<Integer> binary = new ArrayList<>();
        for (int i = 0; i < string.length(); i++) {
            if (tmp[i] == '1') {
                binary.add(1);
            } else if (tmp[i] == '0') {
                binary.add(0);
            }
        }
        return binary;
    }


//	/**
//	 * get result data from model which is the set of existed watermark information
//	 * return WaterMark type
//	 * if random time is more than MAX_TIMES, return null
//	 * @param result dataset from watermark database
//	 * @return WaterMark
//	 */
//	public static Watermark getWaterMark(ArrayList<String> result) throws WatermarkException {
//		for(int new_secret=1; new_secret<MAX_TIMES; new_secret++){
//			ArrayList<Integer> binary = new ArrayList<>();
//			int bits = WATERMARK_SIZE;
//			int x = new_secret;
//			while(bits!=0){
//				int y = x%2;
//				binary.add(y);
//				x/=2;
//				bits--;
//			}
//			if(result.isEmpty()) {
//				return new Watermark(WATERMARK_SIZE,  binary);
//			}
//			boolean ok = true;
//			for(String existedString:result) {
//				ArrayList<Integer> existedBinary = toBinary(existedString);
//				if(BinaryUtils.getSimilarity(binary, existedBinary)>SAME_THRESHOLD) {
//					ok=false;
//					break;
//				}
//			}
//			if(ok) {
//				return new Watermark(WATERMARK_SIZE, binary);
//			}
//		}
//		// 已有水印数量太多
//		throw new WatermarkException("该数据表已经存在"+result.size()+"次水印嵌入");
//	}

    // TODO: Watermark generation
    public static Watermark generateWatermarkFromMessageWithKey(String message, String keyCode) throws WatermarkException {
        try {
            SecretKeyFactory kf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec specs = new PBEKeySpec(message.toCharArray(), StringUtils.decodeFromHex(keyCode), 1024, WATERMARK_LENGTH);
            SecretKey sk = kf.generateSecret(specs);
            byte[] material = sk.getEncoded();
            return new Watermark(WATERMARK_LENGTH, StringUtils.encodeToBitArray(material, WATERMARK_LENGTH, WATERMARK_LENGTH)); // 1 bit for ending
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            log.error("Cannot run KDF: ", e);
            throw new WatermarkException("Cannot generate watermark from message using keycode.");
        }
    }

    public static Watermark generateWatermarkFromMessage(String message) throws WatermarkException {
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > 0 && bytes.length < WATERMARK_LENGTH / Byte.SIZE) {
            int wmLength = bytes.length * Byte.SIZE;
            byte[] material = new byte[bytes.length + 1];
            byte pad = (byte) 0x80;
            System.arraycopy(bytes, 0, material, 0, bytes.length);
            material[bytes.length] = pad;
            return new Watermark(wmLength, StringUtils.encodeToBitArray(material, wmLength + 1, WATERMARK_LENGTH + 1)); // 1 bit for ending
        } else {
            throw new WatermarkException("Cannot generate watermark from message.");
        }
    }
}
