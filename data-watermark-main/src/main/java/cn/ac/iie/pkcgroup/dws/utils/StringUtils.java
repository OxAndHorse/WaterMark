package cn.ac.iie.pkcgroup.dws.utils;

import cn.ac.iie.pkcgroup.dws.utils.crypto.HashUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static cn.ac.iie.pkcgroup.dws.core.db.Constants.PRIMARY_KEY_DIVIDER;

@Slf4j
public class StringUtils {
    public static final String CONN = ".";

    public static String generateIdenticalKey(String dbName, String tableName) {
        return dbName + CONN + tableName;
    }

    public static String generateIdenticalKey(String systemId, String dbName, String tableName) {
        return systemId + CONN + dbName + CONN + tableName;
    }

    public static String generateIdenticalKey(ArrayList<String> materialList) {
        StringBuilder sb = new StringBuilder();
        for (String material :
                materialList) {
            sb.append(material).append(CONN);
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    public static String parseFirstPartFromIdenticalKey(String identicalKey) {
        int connIndex = identicalKey.indexOf(CONN);
        return identicalKey.substring(0, connIndex);
    }

    public static ArrayList<String> parseAllPartsFromIdenticalKey(String identicalKey) {
        ArrayList<String> list = new ArrayList<>();
        String ptr = identicalKey;
        int pos = ptr.indexOf(CONN);
        while (pos >= 0) {
            list.add(ptr.substring(0, pos));
            ptr = ptr.substring(pos + 1);
            pos = ptr.indexOf(CONN);
        }
        list.add(ptr);
        return list;
    }

    public static String encodeToHex(byte[] ori) {
        StringBuilder sb = new StringBuilder();
        for (byte b : ori) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static byte[] decodeFromHex(String str) {
        byte[] bytes = new byte[str.length() / 2];
        for (int index = 0; index < bytes.length; index++) {
            bytes[index] = (byte) (Integer.decode("0x" + str.substring(2 * index, 2 * index + 2)) & 0xff);
        }
        return bytes;
    }

    public static ArrayList<Integer> encodeToBitArray(byte[] ori) {
        ArrayList<Integer> bitsList = new ArrayList<>(ori.length * Byte.SIZE);
        for (int bits : ori) {
            for (int j = 0; j < Byte.SIZE; ++j) {
                if ((bits & 0x01) == 0x01) {
                    bitsList.add(1);
                } else {
                    bitsList.add(0);
                }
                bits = (bits >>> 1) & 0xff;
            }
        }
        return bitsList;
    }

    public static ArrayList<Integer> encodeToBitArray(byte[] ori, int bitLength, int totalLength) {
        ArrayList<Integer> bitsList = new ArrayList<>(bitLength);
        if (bitLength > ori.length * Byte.SIZE) {
            log.error("Too long bit length.");
            return null;
        }
        int restLength = bitLength;
        for (int bits : ori) {
            int unit = Math.min(restLength, Byte.SIZE);
            for (int j = 0; j < unit; ++j) {
                if ((bits & 0x80) == 0x80) {
                    bitsList.add(1);
                } else {
                    bitsList.add(0);
                }
                bits = (bits << 1) & 0xff;
            }
            restLength -= Byte.SIZE;
        }
        for (int i = 0; i < totalLength - bitLength; ++i) {
            bitsList.add(0); // padding with 0
        }
        return bitsList;
    }

    public static String decodeFromBitString(String bitString) {
        int splitSize = Byte.SIZE;
        if (bitString.length() % splitSize == 0) {
            int index = 0;
            int position = 0;

            byte[] resultByteArray = new byte[bitString.length() / splitSize];
            while (index < bitString.length()) {
                String binaryStringChunk = bitString.substring(index, index + splitSize);
                int byteAsInt = Integer.parseInt(binaryStringChunk, 2);
                resultByteArray[position] = (byte) byteAsInt;
                index += splitSize;
                position++;
            }
            return new String(resultByteArray, StandardCharsets.UTF_8);
        } else {
            return null;
        }
    }


    public static String generateDWFileId(String dwFilePath) {
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(dwFilePath));
            return HashUtils.doHashToHex(bytes);
        } catch (IOException e) {
            log.error("Dw file does not exist.");
            return null;
        }
    }

    public static String generateDWFileId(String systemId, String fileName) {
        StringBuilder sb = new StringBuilder();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timestamp = df.format(new Date());
        sb.append(systemId).append(fileName).append(timestamp);
        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        return HashUtils.doHashToHex(bytes);
    }

    public static String generateFileId(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            return HashUtils.doHashToHex(bytes);
        } catch (IOException e) {
            log.error("File does not exist.");
            return null;
        }
    }

    public static String parseFileType(String filePath) {
        String[] list = filePath.split("\\.");
        return list[list.length - 1];
    }

    public static String parseFileName(String filePath) {
        String[] list = filePath.split("/");
        return list[list.length - 1];
    }

    public static String getRowPrimaryKeyValue(ArrayList<Integer> pkIndexList, ArrayList<String> rowData) {
        StringBuilder rowPK = new StringBuilder();
        for (Integer pkIndex :
                pkIndexList) {
            rowPK.append(rowData.get(pkIndex)).append(PRIMARY_KEY_DIVIDER);
        }
        rowPK.deleteCharAt(rowPK.length() - 1);
        return rowPK.toString();
    }
}
