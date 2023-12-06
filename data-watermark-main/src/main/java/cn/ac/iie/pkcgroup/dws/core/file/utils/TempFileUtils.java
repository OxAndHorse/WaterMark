package cn.ac.iie.pkcgroup.dws.core.file.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class TempFileUtils {

    public static String generateTempFilePath(String rootPath, String fileSuffix) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        String timestamp = df.format(new Date());
        String uuid = UUID.randomUUID().toString().replaceAll("-","").substring(0,10);
        return rootPath + "temp" + timestamp + "_" + uuid + fileSuffix;
    }
}
